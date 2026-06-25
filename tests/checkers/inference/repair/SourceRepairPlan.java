package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A ranked source-realizable repair plan over legal edit domains. */
public final class SourceRepairPlan {
    private final int rank;
    private final int totalCost;
    private final List<SourceRepairPlanStep> steps;

    public SourceRepairPlan(int rank, int totalCost, List<SourceRepairPlanStep> steps) {
        this.rank = rank;
        this.totalCost = totalCost;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public int getRank() {
        return rank;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public List<SourceRepairPlanStep> getSteps() {
        return steps;
    }
}
