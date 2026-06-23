package checkers.inference;

/** Thrown when inference completes constraint generation but the solver finds no solution. */
public final class InferenceUnsatisfiableException extends RuntimeException {
    private final InferenceRunSnapshot snapshot;

    public InferenceUnsatisfiableException(InferenceRunSnapshot snapshot) {
        super("Inference solver found no solution");
        this.snapshot = snapshot;
    }

    public InferenceRunSnapshot getSnapshot() {
        return snapshot;
    }
}
