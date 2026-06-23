package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;

import java.io.File;

/** Result of applying one inference-guided repair candidate and rerunning inference. */
public final class InferenceRepairValidationResult {
    private final InferenceRepairCandidate candidate;
    private final File repairedSourceFile;
    private final String appliedEdit;
    private final InferenceRunSnapshot snapshot;

    public InferenceRepairValidationResult(
            InferenceRepairCandidate candidate,
            File repairedSourceFile,
            String appliedEdit,
            InferenceRunSnapshot snapshot) {
        this.candidate = candidate;
        this.repairedSourceFile = repairedSourceFile;
        this.appliedEdit = appliedEdit;
        this.snapshot = snapshot;
    }

    public InferenceRepairCandidate getCandidate() {
        return candidate;
    }

    public File getRepairedSourceFile() {
        return repairedSourceFile;
    }

    public String getAppliedEdit() {
        return appliedEdit;
    }

    public InferenceRunSnapshot getSnapshot() {
        return snapshot;
    }

    public boolean solvesInference() {
        return snapshot != null && snapshot.hasSolution();
    }
}
