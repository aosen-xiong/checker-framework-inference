package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Selects minimum-cost source-realizable repair plans from repair-unit edit domains. */
public final class SourceRepairPlanSelector {
    public List<SourceRepairPlan> selectTopK(List<SourceRepairUnit> repairUnits, int topK) {
        List<UnrankedPlan> candidates = new ArrayList<>();
        for (SourceRepairUnit unit : repairUnits) {
            if (!unit.isSourceRealizable()) {
                continue;
            }
            for (SourceRepairEditOption option : unit.getEditDomain()) {
                if ("Identity".equals(option.getKind())) {
                    continue;
                }
                SourceRepairPlanStep step =
                        new SourceRepairPlanStep(
                                unit.getId(),
                                option.getKind(),
                                option.getQualifier(),
                                option.getCost());
                candidates.add(new UnrankedPlan(option.getCost(), Collections.singletonList(step)));
            }
        }
        Collections.sort(
                candidates,
                Comparator.comparingInt(UnrankedPlan::getTotalCost)
                        .thenComparing(UnrankedPlan::firstUnitId)
                        .thenComparing(UnrankedPlan::firstEditKind));
        List<SourceRepairPlan> ranked = new ArrayList<>();
        int limit = topK <= 0 ? candidates.size() : Math.min(topK, candidates.size());
        for (int index = 0; index < limit; index++) {
            UnrankedPlan candidate = candidates.get(index);
            ranked.add(new SourceRepairPlan(index + 1, candidate.totalCost, candidate.steps));
        }
        return ranked;
    }

    private static final class UnrankedPlan {
        private final int totalCost;
        private final List<SourceRepairPlanStep> steps;

        private UnrankedPlan(int totalCost, List<SourceRepairPlanStep> steps) {
            this.totalCost = totalCost;
            this.steps = steps;
        }

        private int getTotalCost() {
            return totalCost;
        }

        private String firstUnitId() {
            return steps.isEmpty() ? "" : steps.get(0).getRepairUnitId();
        }

        private String firstEditKind() {
            return steps.isEmpty() ? "" : steps.get(0).getEditKind();
        }
    }
}
