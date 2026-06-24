package checkers.inference.repair;

import java.util.List;

/** Proposes inference repair candidates from repair-oriented constraint contexts. */
public interface InferenceRepairCandidatePlanner {
    List<InferenceRepairCandidate> planFromInferenceContexts(
            List<InferenceConstraintContext> contexts);
}
