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

    public InferenceRepairValidationResult(
            InferenceRepairCandidate candidate,
            List<InferenceRepairAttempt> attempts) {
        this.candidate = candidate;
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
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
