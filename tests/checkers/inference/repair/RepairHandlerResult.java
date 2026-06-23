package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of running one repair handler over its supported diagnostics. */
public final class RepairHandlerResult {
    private final String handlerName;
    private final List<RepairDiagnostic> diagnostics;
    private final RepairBatchValidationResult annotationValidationResult;
    private final List<CodeRepairValidationResult> codeValidationResults;

    private RepairHandlerResult(
            String handlerName,
            List<RepairDiagnostic> diagnostics,
            RepairBatchValidationResult annotationValidationResult,
            List<CodeRepairValidationResult> codeValidationResults) {
        this.handlerName = handlerName;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
        this.annotationValidationResult = annotationValidationResult;
        this.codeValidationResults =
                Collections.unmodifiableList(new ArrayList<>(codeValidationResults));
    }

    public static RepairHandlerResult forAnnotationRepair(
            String handlerName,
            List<RepairDiagnostic> diagnostics,
            RepairBatchValidationResult validationResult) {
        return new RepairHandlerResult(
                handlerName, diagnostics, validationResult, Collections.emptyList());
    }

    public static RepairHandlerResult forCodeRepair(
            String handlerName,
            List<RepairDiagnostic> diagnostics,
            List<CodeRepairValidationResult> validationResults) {
        return new RepairHandlerResult(handlerName, diagnostics, null, validationResults);
    }

    public String getHandlerName() {
        return handlerName;
    }

    public List<RepairDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public RepairBatchValidationResult getAnnotationValidationResult() {
        return annotationValidationResult;
    }

    public List<CodeRepairValidationResult> getCodeValidationResults() {
        return codeValidationResults;
    }

    public boolean removesAllDiagnostics() {
        if (annotationValidationResult != null) {
            return annotationValidationResult.removesAllDiagnostics();
        }
        for (CodeRepairValidationResult validationResult : codeValidationResults) {
            if (!validationResult.removesAllDiagnostics()) {
                return false;
            }
        }
        return !codeValidationResults.isEmpty();
    }
}
