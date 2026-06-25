package checkers.inference.repair;

import java.io.File;

/** Top-level metrics extracted from one experiment JSON report. */
public final class InferenceRepairExperimentSummary {
    private final File reportFile;
    private final String projectName;
    private final String revision;
    private final String mode;
    private final String editProviderMode;
    private final int inputCount;
    private final int runErrorCount;
    private final int initialInferenceSolvedCount;
    private final int repairSolvedInferenceCount;
    private final int repairFullyVerifiedCount;
    private final int candidateCount;
    private final int attemptCount;

    public InferenceRepairExperimentSummary(
            File reportFile,
            String projectName,
            String revision,
            String mode,
            String editProviderMode,
            int inputCount,
            int runErrorCount,
            int initialInferenceSolvedCount,
            int repairSolvedInferenceCount,
            int repairFullyVerifiedCount,
            int candidateCount,
            int attemptCount) {
        this.reportFile = reportFile;
        this.projectName = projectName;
        this.revision = revision;
        this.mode = mode;
        this.editProviderMode = editProviderMode;
        this.inputCount = inputCount;
        this.runErrorCount = runErrorCount;
        this.initialInferenceSolvedCount = initialInferenceSolvedCount;
        this.repairSolvedInferenceCount = repairSolvedInferenceCount;
        this.repairFullyVerifiedCount = repairFullyVerifiedCount;
        this.candidateCount = candidateCount;
        this.attemptCount = attemptCount;
    }

    public static String csvHeader() {
        return "reportFile,projectName,revision,mode,editProviderMode,inputCount,runErrorCount,"
                + "initialInferenceSolvedCount,repairSolvedInferenceCount,"
                + "repairFullyVerifiedCount,candidateCount,attemptCount";
    }

    public String toCsvRow() {
        return csv(reportFile.getPath())
                + ","
                + csv(projectName)
                + ","
                + csv(revision)
                + ","
                + csv(mode)
                + ","
                + csv(editProviderMode)
                + ","
                + inputCount
                + ","
                + runErrorCount
                + ","
                + initialInferenceSolvedCount
                + ","
                + repairSolvedInferenceCount
                + ","
                + repairFullyVerifiedCount
                + ","
                + candidateCount
                + ","
                + attemptCount;
    }

    private static String csv(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
