package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of applying one inference-guided repair candidate and rerunning inference. */
public final class InferenceRepairValidationResult {
    private final InferenceRepairCandidate candidate;
    private final List<InferenceRepairAttempt> attempts;
    private final RuntimeException validationError;

    public InferenceRepairValidationResult(
            InferenceRepairCandidate candidate,
            List<InferenceRepairAttempt> attempts) {
        this(candidate, attempts, null);
    }

    private InferenceRepairValidationResult(
            InferenceRepairCandidate candidate,
            List<InferenceRepairAttempt> attempts,
            RuntimeException validationError) {
        this.candidate = candidate;
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
        this.validationError = validationError;
    }

    public static InferenceRepairValidationResult failed(
            InferenceRepairCandidate candidate, RuntimeException validationError) {
        return new InferenceRepairValidationResult(
                candidate, Collections.<InferenceRepairAttempt>emptyList(), validationError);
    }

    public InferenceRepairCandidate getCandidate() {
        return candidate;
    }

    public File getRepairedSourceFile() {
        return requirePassingAttempt().getRepairedSourceFile();
    }

    public String getAppliedEdit() {
        return requirePassingAttempt().getAppliedEdit();
    }

    public InferenceRunSnapshot getSnapshot() {
        return requirePassingAttempt().getSnapshot();
    }

    public List<InferenceRepairAttempt> getAttempts() {
        return attempts;
    }

    public RuntimeException getValidationError() {
        return validationError;
    }

    public boolean hasValidationError() {
        return validationError != null;
    }

    public boolean solvesInference() {
        return getPassingAttempt() != null;
    }

    public InferenceRepairAttempt getPassingAttempt() {
        for (InferenceRepairAttempt attempt : attempts) {
            if (attempt.solvesInference()) {
                return attempt;
            }
        }
        return null;
    }

    private InferenceRepairAttempt requirePassingAttempt() {
        InferenceRepairAttempt passingAttempt = getPassingAttempt();
        if (passingAttempt == null) {
            throw new IllegalStateException("No repair attempt solved inference.");
        }
        return passingAttempt;
    }
}
