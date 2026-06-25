package checkers.inference.repair;

import java.io.File;

/** Project-level metadata attached to an experiment batch report. */
public final class InferenceRepairExperimentMetadata {
    private final String projectName;
    private final File projectRoot;
    private final String revision;

    public InferenceRepairExperimentMetadata(String projectName, File projectRoot, String revision) {
        this.projectName = projectName;
        this.projectRoot = projectRoot;
        this.revision = revision;
    }

    public static InferenceRepairExperimentMetadata empty() {
        return new InferenceRepairExperimentMetadata("", null, "");
    }

    public String getProjectName() {
        return projectName;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public String getRevision() {
        return revision;
    }

    public String projectRootPath() {
        return projectRoot == null ? "" : projectRoot.getPath();
    }
}
