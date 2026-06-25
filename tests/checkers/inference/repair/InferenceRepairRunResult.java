package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;

/** Inference and post-verification result for one repaired source attempt. */
public final class InferenceRepairRunResult {
    private final InferenceRunSnapshot snapshot;
    private final InferenceRepairPostVerificationResult postVerificationResult;

    public InferenceRepairRunResult(
            InferenceRunSnapshot snapshot,
            InferenceRepairPostVerificationResult postVerificationResult) {
        this.snapshot = snapshot;
        this.postVerificationResult = postVerificationResult;
    }

    public InferenceRunSnapshot getSnapshot() {
        return snapshot;
    }

    public InferenceRepairPostVerificationResult getPostVerificationResult() {
        return postVerificationResult;
    }
}
