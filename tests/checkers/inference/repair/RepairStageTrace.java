package checkers.inference.repair;

import java.io.File;

/** One step in a staged inference-and-source-repair validation pipeline. */
public final class RepairStageTrace {
    private final String stage;
    private final String action;
    private final boolean attempted;
    private final boolean succeeded;
    private final boolean verified;
    private final File outputFile;
    private final String appliedEdit;
    private final String replacementSource;
    private final String error;

    public RepairStageTrace(
            String stage,
            String action,
            boolean attempted,
            boolean succeeded,
            boolean verified,
            File outputFile,
            String appliedEdit,
            String replacementSource,
            String error) {
        this.stage = stage;
        this.action = action;
        this.attempted = attempted;
        this.succeeded = succeeded;
        this.verified = verified;
        this.outputFile = outputFile;
        this.appliedEdit = appliedEdit;
        this.replacementSource = replacementSource;
        this.error = error;
    }

    public String getStage() {
        return stage;
    }

    public String getAction() {
        return action;
    }

    public boolean isAttempted() {
        return attempted;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public boolean isVerified() {
        return verified;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getAppliedEdit() {
        return appliedEdit;
    }

    public String getReplacementSource() {
        return replacementSource;
    }

    public String getError() {
        return error;
    }
}
