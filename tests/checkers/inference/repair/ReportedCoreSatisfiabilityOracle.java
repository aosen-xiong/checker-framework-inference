package checkers.inference.repair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Oracle that treats the solver-reported core as an unsatisfiable constraint set. */
public final class ReportedCoreSatisfiabilityOracle
        implements InferenceConstraintSatisfiabilityOracle {
    private final Set<String> unsatCoreKeys = new HashSet<>();

    public ReportedCoreSatisfiabilityOracle(List<InferenceConstraintContext> unsatCoreContexts) {
        for (InferenceConstraintContext context : unsatCoreContexts) {
            unsatCoreKeys.add(context.summarize());
        }
    }

    @Override
    public boolean isSatisfiable(List<InferenceConstraintContext> retainedContexts) {
        if (unsatCoreKeys.isEmpty()) {
            return true;
        }
        Set<String> retainedKeys = new HashSet<>();
        for (InferenceConstraintContext context : retainedContexts) {
            retainedKeys.add(context.summarize());
        }
        return !retainedKeys.containsAll(unsatCoreKeys);
    }
}
