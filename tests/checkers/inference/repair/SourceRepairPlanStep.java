package checkers.inference.repair;

/** One selected edit in a source-realizable repair plan. */
public final class SourceRepairPlanStep {
    private final String repairUnitId;
    private final String editKind;
    private final String qualifier;
    private final int cost;

    public SourceRepairPlanStep(String repairUnitId, String editKind, String qualifier, int cost) {
        this.repairUnitId = repairUnitId;
        this.editKind = editKind;
        this.qualifier = qualifier;
        this.cost = cost;
    }

    public String getRepairUnitId() {
        return repairUnitId;
    }

    public String getEditKind() {
        return editKind;
    }

    public String getQualifier() {
        return qualifier;
    }

    public int getCost() {
        return cost;
    }
}
