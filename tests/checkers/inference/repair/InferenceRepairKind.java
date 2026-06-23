package checkers.inference.repair;

/** Kinds of source edits available to inference-guided repair search. */
public enum InferenceRepairKind {
    WEAKEN_ANNOTATION,
    INSERT_NULL_GUARD,
    REPLACE_WITH_NONNULL_FALLBACK
}
