package checkers.inference.repair;

/**
 * Marker interface for AI-backed repair edit providers.
 *
 * <p>AI providers should only propose edits. The generic repair validator remains responsible for
 * applying each edit and accepting it only after inference, AFU insertion, and final typecheck all
 * pass.
 */
public interface AiInferenceRepairEditProvider extends InferenceRepairEditProvider {}
