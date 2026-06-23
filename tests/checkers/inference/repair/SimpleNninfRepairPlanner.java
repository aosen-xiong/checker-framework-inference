package checkers.inference.repair;

import static checkers.inference.repair.SimpleNninfUnsatCoreSolver.NULLABLE;

import java.util.ArrayList;
import java.util.List;

/** Proposes local nninf repairs from unsat-core constraints. */
public final class SimpleNninfRepairPlanner {
    public List<RepairCandidate> plan(UnsatCoreResult coreResult) {
        List<RepairCandidate> candidates = new ArrayList<>();
        for (RepairConstraint constraint : coreResult.getUnsatCore()) {
            candidates.add(
                    new RepairCandidate(
                            constraint,
                            constraint.getSupertype(),
                            NULLABLE,
                            "weaken target slot to @Nullable"));
        }
        return candidates;
    }
}
