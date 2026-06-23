package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;

import java.io.File;

/** One attempted source edit and its inference rerun result. */
public final class InferenceRepairAttempt {
    private final InferenceRepairKind repairKind;
    private final File repairedSourceFile;
    private final String appliedEdit;
    private final InferenceRunSnapshot snapshot;

    public InferenceRepairAttempt(
            InferenceRepairKind repairKind,
            File repairedSourceFile,
            String appliedEdit,
            InferenceRunSnapshot snapshot) {
        this.repairKind = repairKind;
        this.repairedSourceFile = repairedSourceFile;
        this.appliedEdit = appliedEdit;
        this.snapshot = snapshot;
    }

    public InferenceRepairKind getRepairKind() {
        return repairKind;
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
