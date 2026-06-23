package checkers.inference.repair;

/** A source edit proposed to repair an unsat-core constraint. */
public final class RepairCandidate {
    private final RepairConstraint constraint;
    private final RepairSlot targetSlot;
    private final String qualifier;
    private final String description;

    public RepairCandidate(
            RepairConstraint constraint, RepairSlot targetSlot, String qualifier, String description) {
        this.constraint = constraint;
        this.targetSlot = targetSlot;
        this.qualifier = qualifier;
        this.description = description;
    }

    public RepairConstraint getConstraint() {
        return constraint;
    }

    public RepairSlot getTargetSlot() {
        return targetSlot;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getDescription() {
        return description;
    }
}
