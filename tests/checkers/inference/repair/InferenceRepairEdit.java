package checkers.inference.repair;

/** One concrete source edit that can be validated by rerunning inference. */
public final class InferenceRepairEdit {
    private final InferenceRepairKind repairKind;
    private final String replacementSource;
    private final String description;
    private final String directoryName;

    public InferenceRepairEdit(
            InferenceRepairKind repairKind,
            String replacementSource,
            String description,
            String directoryName) {
        this.repairKind = repairKind;
        this.replacementSource = replacementSource;
        this.description = description;
        this.directoryName = directoryName;
    }

    public InferenceRepairKind getRepairKind() {
        return repairKind;
    }

    public String getReplacementSource() {
        return replacementSource;
    }

    public String getDescription() {
        return description;
    }

    public String getDirectoryName() {
        return directoryName;
    }
}
