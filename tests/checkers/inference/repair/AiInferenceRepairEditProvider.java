package checkers.inference.repair;

import java.util.List;

/**
 * Marker interface for AI-backed repair edit providers.
 *
 * <p>AI providers should only propose edits. The generic repair validator remains responsible for
 * applying each edit and accepting it only after inference, AFU insertion, and final typecheck all
 * pass.
 */
public interface AiInferenceRepairEditProvider extends InferenceRepairEditProvider {
    @Override
    default List<InferenceRepairEdit> generate(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource) {
        return generate(RepairPromptContext.create(candidate, target, originalSource));
    }

    List<InferenceRepairEdit> generate(RepairPromptContext context);
}
