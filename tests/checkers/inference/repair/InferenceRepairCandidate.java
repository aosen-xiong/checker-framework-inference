package checkers.inference.repair;

/** A repair candidate proposed directly from an inference unsat-constraint context. */
public final class InferenceRepairCandidate {
    private final InferenceConstraintContext constraintContext;
    private final InferenceSlotContext targetSlot;
    private final String qualifier;
    private final String description;

    public InferenceRepairCandidate(
            InferenceConstraintContext constraintContext,
            InferenceSlotContext targetSlot,
            String qualifier,
            String description) {
        this.constraintContext = constraintContext;
        this.targetSlot = targetSlot;
        this.qualifier = qualifier;
        this.description = description;
    }

    public InferenceConstraintContext getConstraintContext() {
        return constraintContext;
    }

    public InferenceSlotContext getTargetSlot() {
        return targetSlot;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getDescription() {
        return description;
    }

    public String summarize() {
        return description
                + " at "
                + targetSlot.getDescription()
                + " ["
                + targetSlot.getLocationKind()
                + ": "
                + targetSlot.getLocation()
                + "]"
                + " because "
                + constraintContext.getRelation();
    }
}
