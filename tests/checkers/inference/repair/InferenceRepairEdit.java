package checkers.inference.repair;

/** One concrete source edit that can be validated by rerunning inference. */
public final class InferenceRepairEdit {
    private final InferenceRepairKind repairKind;
    private final String replacementSource;
    private final String description;
    private final String directoryName;
    private final InferenceRepairEditOrigin origin;

    public InferenceRepairEdit(
            InferenceRepairKind repairKind,
            String replacementSource,
            String description,
            String directoryName) {
        this(
                repairKind,
                replacementSource,
                description,
                directoryName,
                InferenceRepairEditOrigin.DETERMINISTIC);
    }

    public InferenceRepairEdit(
            InferenceRepairKind repairKind,
            String replacementSource,
            String description,
            String directoryName,
            InferenceRepairEditOrigin origin) {
        this.repairKind = repairKind;
        this.replacementSource = replacementSource;
        this.description = description;
        this.directoryName = directoryName;
        this.origin = origin;
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

    public InferenceRepairEditOrigin getOrigin() {
        return origin;
    }
}
