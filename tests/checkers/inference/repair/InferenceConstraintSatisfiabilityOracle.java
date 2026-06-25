package checkers.inference.repair;

import java.util.List;

/** Satisfiability oracle over retained inference constraint contexts. */
public interface InferenceConstraintSatisfiabilityOracle {
    boolean isSatisfiable(List<InferenceConstraintContext> retainedContexts);
}
