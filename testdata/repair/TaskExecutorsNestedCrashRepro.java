import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

interface NestedCrashTaskProcessor<T> {
    enum ProcessingResult {
        Success,
        TransientError,
        PermanentError,
        Congestion
    }

    ProcessingResult process(T task);

    ProcessingResult process(List<T> tasks);
}

interface NestedCrashAcceptorExecutor<ID, T> {
    BlockingQueue<NestedCrashTaskHolder<ID, T>> requestWorkItem();

    BlockingQueue<List<NestedCrashTaskHolder<ID, T>>> requestWorkItems();

    void reprocess(NestedCrashTaskHolder<ID, T> holder, NestedCrashTaskProcessor.ProcessingResult result);

    void reprocess(
            List<NestedCrashTaskHolder<ID, T>> holders,
            NestedCrashTaskProcessor.ProcessingResult result);
}

class NestedCrashTaskHolder<ID, T> {
    long getSubmitTimestamp() {
        return 0L;
    }

    T getTask() {
        return null;
    }
}

class TaskExecutorsNestedCrashRepro<ID, T> {
    TaskExecutorsNestedCrashRepro(
            WorkerRunnableFactory<ID, T> workerRunnableFactory,
            int workerCount,
            AtomicBoolean isShutdown) {
        for (int i = 0; i < workerCount; i++) {
            WorkerRunnable<ID, T> runnable = workerRunnableFactory.create(i);
            Thread workerThread = new Thread(runnable, runnable.getWorkerName());
            workerThread.start();
        }
    }

    static <ID, T> TaskExecutorsNestedCrashRepro<ID, T> singleItemExecutors(
            final String name,
            int workerCount,
            final NestedCrashTaskProcessor<T> processor,
            final NestedCrashAcceptorExecutor<ID, T> acceptorExecutor) {
        final AtomicBoolean isShutdown = new AtomicBoolean();
        final TaskExecutorMetrics metrics = new TaskExecutorMetrics();
        return new TaskExecutorsNestedCrashRepro<>(
                idx ->
                        new SingleTaskWorkerRunnable<>(
                                "single-" + name + '-' + idx,
                                isShutdown,
                                metrics,
                                processor,
                                acceptorExecutor),
                workerCount,
                isShutdown);
    }

    static <ID, T> TaskExecutorsNestedCrashRepro<ID, T> batchExecutors(
            final String name,
            int workerCount,
            final NestedCrashTaskProcessor<T> processor,
            final NestedCrashAcceptorExecutor<ID, T> acceptorExecutor) {
        final AtomicBoolean isShutdown = new AtomicBoolean();
        final TaskExecutorMetrics metrics = new TaskExecutorMetrics();
        return new TaskExecutorsNestedCrashRepro<>(
                idx ->
                        new BatchWorkerRunnable<>(
                                "batch-" + name + '-' + idx,
                                isShutdown,
                                metrics,
                                processor,
                                acceptorExecutor),
                workerCount,
                isShutdown);
    }

    static class TaskExecutorMetrics {
        void registerTaskResult(NestedCrashTaskProcessor.ProcessingResult result, int count) {}

        <ID, T> void registerExpiryTime(NestedCrashTaskHolder<ID, T> holder) {}

        <ID, T> void registerExpiryTimes(List<NestedCrashTaskHolder<ID, T>> holders) {}
    }

    interface WorkerRunnableFactory<ID, T> {
        WorkerRunnable<ID, T> create(int idx);
    }

    abstract static class WorkerRunnable<ID, T> implements Runnable {
        final String workerName;
        final AtomicBoolean isShutdown;
        final TaskExecutorMetrics metrics;
        final NestedCrashTaskProcessor<T> processor;
        final NestedCrashAcceptorExecutor<ID, T> taskDispatcher;

        WorkerRunnable(
                String workerName,
                AtomicBoolean isShutdown,
                TaskExecutorMetrics metrics,
                NestedCrashTaskProcessor<T> processor,
                NestedCrashAcceptorExecutor<ID, T> taskDispatcher) {
            this.workerName = workerName;
            this.isShutdown = isShutdown;
            this.metrics = metrics;
            this.processor = processor;
            this.taskDispatcher = taskDispatcher;
        }

        String getWorkerName() {
            return workerName;
        }
    }

    static class BatchWorkerRunnable<ID, T> extends WorkerRunnable<ID, T> {
        BatchWorkerRunnable(
                String workerName,
                AtomicBoolean isShutdown,
                TaskExecutorMetrics metrics,
                NestedCrashTaskProcessor<T> processor,
                NestedCrashAcceptorExecutor<ID, T> acceptorExecutor) {
            super(workerName, isShutdown, metrics, processor, acceptorExecutor);
        }

        public void run() {
            try {
                while (!isShutdown.get()) {
                    List<NestedCrashTaskHolder<ID, T>> holders = getWork();
                    metrics.registerExpiryTimes(holders);

                    List<T> tasks = getTasksOf(holders);
                    NestedCrashTaskProcessor.ProcessingResult result = processor.process(tasks);
                    switch (result) {
                        case Success:
                            break;
                        case Congestion:
                        case TransientError:
                            taskDispatcher.reprocess(holders, result);
                            break;
                        case PermanentError:
                            break;
                    }
                    metrics.registerTaskResult(result, tasks.size());
                }
            } catch (InterruptedException e) {
            }
        }

        private List<NestedCrashTaskHolder<ID, T>> getWork() throws InterruptedException {
            BlockingQueue<List<NestedCrashTaskHolder<ID, T>>> workQueue =
                    taskDispatcher.requestWorkItems();
            List<NestedCrashTaskHolder<ID, T>> result;
            do {
                result = workQueue.poll(1, TimeUnit.SECONDS);
            } while (!isShutdown.get() && result == null);
            return (result == null) ? new ArrayList<>() : result;
        }

        private List<T> getTasksOf(List<NestedCrashTaskHolder<ID, T>> holders) {
            List<T> tasks = new ArrayList<>(holders.size());
            for (NestedCrashTaskHolder<ID, T> holder : holders) {
                tasks.add(holder.getTask());
            }
            return tasks;
        }
    }

    static class SingleTaskWorkerRunnable<ID, T> extends WorkerRunnable<ID, T> {
        SingleTaskWorkerRunnable(
                String workerName,
                AtomicBoolean isShutdown,
                TaskExecutorMetrics metrics,
                NestedCrashTaskProcessor<T> processor,
                NestedCrashAcceptorExecutor<ID, T> acceptorExecutor) {
            super(workerName, isShutdown, metrics, processor, acceptorExecutor);
        }

        public void run() {
            try {
                while (!isShutdown.get()) {
                    BlockingQueue<NestedCrashTaskHolder<ID, T>> workQueue =
                            taskDispatcher.requestWorkItem();
                    NestedCrashTaskHolder<ID, T> taskHolder;
                    while ((taskHolder = workQueue.poll(1, TimeUnit.SECONDS)) == null) {
                        if (isShutdown.get()) {
                            return;
                        }
                    }
                    metrics.registerExpiryTime(taskHolder);
                    if (taskHolder != null) {
                        NestedCrashTaskProcessor.ProcessingResult result =
                                processor.process(taskHolder.getTask());
                        switch (result) {
                            case Success:
                                break;
                            case Congestion:
                            case TransientError:
                                taskDispatcher.reprocess(taskHolder, result);
                                break;
                            case PermanentError:
                                break;
                        }
                        metrics.registerTaskResult(result, 1);
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
