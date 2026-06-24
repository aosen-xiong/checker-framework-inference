package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of trying ranked inference repair candidates until one solves inference. */
public final class InferenceRepairSearchResult {
    private final List<InferenceRepairValidationResult> validationResults;

    public InferenceRepairSearchResult(List<InferenceRepairValidationResult> validationResults) {
        this.validationResults = Collections.unmodifiableList(new ArrayList<>(validationResults));
    }

    public List<InferenceRepairValidationResult> getValidationResults() {
        return validationResults;
    }

    public boolean solvesInference() {
        return getPassingResult() != null;
    }

    public boolean isFullyVerified() {
        return getVerifiedResult() != null;
    }

    public InferenceRepairValidationResult getPassingResult() {
        for (InferenceRepairValidationResult validationResult : validationResults) {
            if (validationResult.solvesInference()) {
                return validationResult;
            }
        }
        return null;
    }

    public InferenceRepairValidationResult getVerifiedResult() {
        for (InferenceRepairValidationResult validationResult : validationResults) {
            if (validationResult.isFullyVerified()) {
                return validationResult;
            }
        }
        return null;
    }
}
