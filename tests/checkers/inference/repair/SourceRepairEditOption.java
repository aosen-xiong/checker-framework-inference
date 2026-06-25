package checkers.inference.repair;

/** One legal source edit option for a repair unit. */
public final class SourceRepairEditOption {
    private final String kind;
    private final String qualifier;
    private final int cost;

    public SourceRepairEditOption(String kind, String qualifier, int cost) {
        this.kind = kind;
        this.qualifier = qualifier;
        this.cost = cost;
    }

    public String getKind() {
        return kind;
    }

    public String getQualifier() {
        return qualifier;
    }

    public int getCost() {
        return cost;
    }
}
