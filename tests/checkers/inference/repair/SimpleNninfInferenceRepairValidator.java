package checkers.inference.repair;

import java.io.File;
import java.util.List;

/** Convenience wrapper for running the generic inference repair validator with nninf defaults. */
public final class SimpleNninfInferenceRepairValidator {
    private final InferenceRepairValidator validator;

    public SimpleNninfInferenceRepairValidator(File originalSourceFile, File outputDirectory) {
        this.validator =
                new InferenceRepairValidator(
                        InferenceRepairConfiguration.nninfDefault(),
                        originalSourceFile,
                        outputDirectory);
    }

    public InferenceRepairValidationResult validate(InferenceRepairCandidate candidate) {
        return validator.validate(candidate);
    }

    public InferenceRepairSearchResult validateAll(List<InferenceRepairCandidate> candidates) {
        return validator.validateAll(candidates);
    }
}
