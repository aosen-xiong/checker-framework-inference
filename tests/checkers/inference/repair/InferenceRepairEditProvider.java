package checkers.inference.repair;

import java.util.List;

/** Produces concrete source edits for one resolved inference repair target. */
public interface InferenceRepairEditProvider {
    List<InferenceRepairEdit> generate(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource);
}
