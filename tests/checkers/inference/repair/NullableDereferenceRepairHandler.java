package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Handles nullable dereference diagnostics with source-code repairs. */
public final class NullableDereferenceRepairHandler implements RepairHandler {
    private final SimpleNninfCodeRepairPlanner planner = new SimpleNninfCodeRepairPlanner();
    private final SimpleNninfCodeRepairValidator validator;

    public NullableDereferenceRepairHandler(
            Class<?> checker, List<String> javacOptions, File validationOutputDirectory) {
        this.validator =
                new SimpleNninfCodeRepairValidator(checker, javacOptions, validationOutputDirectory);
    }

    @Override
    public String getName() {
        return "nullable-dereference";
    }

    @Override
    public boolean supports(RepairDiagnostic diagnostic) {
        return diagnostic.getKey().equals("dereference.of.nullable");
    }

    @Override
    public RepairHandlerResult repair(List<RepairDiagnostic> diagnostics) {
        List<RepairDiagnostic> supportedDiagnostics = supportedDiagnostics(diagnostics);
        List<CodeRepairCandidate> candidates = planner.plan(supportedDiagnostics);
        List<CodeRepairValidationResult> validationResults = new ArrayList<>();
        for (CodeRepairCandidate candidate : candidates) {
            validationResults.add(validator.validate(candidate));
        }
        return RepairHandlerResult.forCodeRepair(
                getName(), supportedDiagnostics, validationResults);
    }

    private List<RepairDiagnostic> supportedDiagnostics(List<RepairDiagnostic> diagnostics) {
        List<RepairDiagnostic> supportedDiagnostics = new ArrayList<>();
        for (RepairDiagnostic diagnostic : diagnostics) {
            if (supports(diagnostic)) {
                supportedDiagnostics.add(diagnostic);
            }
        }
        return supportedDiagnostics;
    }
}
