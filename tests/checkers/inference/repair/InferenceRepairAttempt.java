package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;

import java.io.File;

/** One attempted source edit and its inference rerun result. */
public final class InferenceRepairAttempt {
    private final InferenceRepairKind repairKind;
    private final InferenceRepairTarget target;
    private final File repairedSourceFile;
    private final String appliedEdit;
    private final String replacementSource;
    private final InferenceRepairEditOrigin editOrigin;
    private final InferenceRunSnapshot snapshot;
    private final InferenceRepairPostVerificationResult postVerificationResult;

    public InferenceRepairAttempt(
            InferenceRepairKind repairKind,
            InferenceRepairTarget target,
            File repairedSourceFile,
            String appliedEdit,
            String replacementSource,
            InferenceRepairEditOrigin editOrigin,
            InferenceRunSnapshot snapshot,
            InferenceRepairPostVerificationResult postVerificationResult) {
        this.repairKind = repairKind;
        this.target = target;
        this.repairedSourceFile = repairedSourceFile;
        this.appliedEdit = appliedEdit;
        this.replacementSource = replacementSource;
        this.editOrigin = editOrigin;
        this.snapshot = snapshot;
        this.postVerificationResult = postVerificationResult;
    }

    public InferenceRepairKind getRepairKind() {
        return repairKind;
    }

    public InferenceRepairTarget getTarget() {
        return target;
    }

    public File getRepairedSourceFile() {
        return repairedSourceFile;
    }

    public String getAppliedEdit() {
        return appliedEdit;
    }

    public String getReplacementSource() {
        return replacementSource;
    }

    public InferenceRepairEditOrigin getEditOrigin() {
        return editOrigin;
    }

    public InferenceRunSnapshot getSnapshot() {
        return snapshot;
    }

    public InferenceRepairPostVerificationResult getPostVerificationResult() {
        return postVerificationResult;
    }

    public boolean solvesInference() {
        return snapshot != null && snapshot.hasSolution();
    }

    public boolean isFullyVerified() {
        return solvesInference()
                && postVerificationResult != null
                && postVerificationResult.isVerified();
    }
}
