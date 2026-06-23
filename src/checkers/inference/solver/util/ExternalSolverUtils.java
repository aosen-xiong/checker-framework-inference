package checkers.inference.solver.util;

import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.UserError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Utility class with methods to run an external solver program.
 *
 * @see FileUtils
 */
public class ExternalSolverUtils {

    public static final Logger logger = Logger.getLogger(ExternalSolverUtils.class.getName());

    /**
     * Runs the external solver as given by command and uses the given stdOutHandler and
     * stdErrHandler lambdas to process stdOut and stdErr.
     *
     * @param command an external solver command to be executed, with each array entry passed as a
     *     separate process argument.
     * @param stdOutHandler a lambda which takes a {@link BufferedReader} providing the stdOut of
     *     the external solver and handles the stdOut.
     * @param stdErrHandler a lambda which takes a {@link BufferedReader} providing the stdErr of
     *     the external solver and handles the stdErr.
     * @return the exit status code of the external command.
     */
    public static int runExternalSolver(
            String[] command,
            Consumer<BufferedReader> stdOutHandler,
            Consumer<BufferedReader> stdErrHandler) {

        String printableCommand = String.join(" ", command);
        logger.info("Running external solver command \"" + printableCommand + "\".");

        // Start the external solver process
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new UserError(
                    "Could not run external solver command \"%s\": %s",
                    printableCommand, e.getMessage());
        }

        // Create threads to handle stdOut and stdErr
        StdHandlerThread stdOutHandlerThread =
                new StdHandlerThread(process.getInputStream(), stdOutHandler);
        StdHandlerThread stdErrHandlerThread =
                new StdHandlerThread(process.getErrorStream(), stdErrHandler);
        stdOutHandlerThread.start();
        stdErrHandlerThread.start();

        // Wait for external solver threads to finish
        waitForHandler(stdOutHandlerThread, "stdOut");
        waitForHandler(stdErrHandlerThread, "stdErr");

        int exitStatus;
        try {
            exitStatus = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BugInCF("The thread for the external solver was interrupted.");
        }

        logger.info("External solver process finished");

        return exitStatus;
    }

    private static void waitForHandler(StdHandlerThread handlerThread, String streamName) {
        try {
            handlerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BugInCF(
                    "The thread for handling %s of the external solver was interrupted.",
                    streamName);
        }
        handlerThread.throwIfFailed(streamName);
    }

    /**
     * A thread which wraps an InputStream in a BufferedReader and tasks the lambda function to
     * handle the outputs.
     */
    private static class StdHandlerThread extends Thread {
        private final InputStream stream;
        private final Consumer<BufferedReader> handler;
        private RuntimeException failure;

        public StdHandlerThread(final InputStream stream, final Consumer<BufferedReader> handler) {
            this.stream = stream;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                handler.accept(new BufferedReader(new InputStreamReader(stream)));
            } catch (RuntimeException e) {
                failure = e;
            }
        }

        public void throwIfFailed(String streamName) {
            if (failure != null) {
                throw new BugInCF(
                        failure, "External solver %s handler failed: %s", streamName, failure);
            }
        }
    }

    /**
     * A default implementation of a handler which prints any content from the given {@link
     * BufferedReader} to the given stream.
     *
     * @param stream an output stream to print the contents of the reader to.
     * @param stdReader a BufferedReader containing the contents of an external process's std output
     *     stream.
     */
    public static void printStdStream(PrintStream stream, BufferedReader stdReader) {
        String line;
        try {
            while ((line = stdReader.readLine()) != null) {
                stream.println(line);
            }
        } catch (IOException e) {
            throw new BugInCF(e, "Could not read external solver output.");
        }
    }
}
