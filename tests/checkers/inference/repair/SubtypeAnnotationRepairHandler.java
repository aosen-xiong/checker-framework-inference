package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Handles subtype diagnostics by planning annotation edits from unsat-core constraints. */
public final class SubtypeAnnotationRepairHandler implements RepairHandler {
    private final SimpleNninfConstraintExtractor extractor = new SimpleNninfConstraintExtractor();
    private final SimpleNninfUnsatCoreSolver solver = new SimpleNninfUnsatCoreSolver();
    private final SimpleNninfRepairPlanner planner = new SimpleNninfRepairPlanner();
    private final SimpleNninfRepairValidator validator;

    public SubtypeAnnotationRepairHandler(
            Class<?> checker, List<String> javacOptions, File validationOutputDirectory) {
        this.validator =
                new SimpleNninfRepairValidator(checker, javacOptions, validationOutputDirectory);
    }

    @Override
    public String getName() {
        return "subtype-annotation";
    }

    @Override
    public boolean supports(RepairDiagnostic diagnostic) {
        return extractor.supports(diagnostic);
    }

    @Override
    public RepairHandlerResult repair(List<RepairDiagnostic> diagnostics) {
        List<RepairDiagnostic> supportedDiagnostics = supportedDiagnostics(diagnostics);
        List<RepairConstraint> constraints = extractor.extract(supportedDiagnostics);
        UnsatCoreResult coreResult = solver.solve(constraints);
        List<RepairCandidate> candidates = planner.plan(coreResult);
        RepairBatchValidationResult validationResult =
                candidates.isEmpty() ? null : validator.validateAll(candidates);
        return RepairHandlerResult.forAnnotationRepair(
                getName(), supportedDiagnostics, validationResult);
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
