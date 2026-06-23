package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Runs the simple nninf repair prototype without mutating original sources. */
public final class SimpleNninfRepairDryRunner {
    private final Class<?> checker;
    private final List<String> javacOptions;
    private final SimpleNninfConstraintExtractor extractor = new SimpleNninfConstraintExtractor();
    private final SimpleNninfUnsatCoreSolver solver = new SimpleNninfUnsatCoreSolver();
    private final SimpleNninfRepairPlanner planner = new SimpleNninfRepairPlanner();
    private final SimpleNninfRepairValidator validator;

    public SimpleNninfRepairDryRunner(
            Class<?> checker, List<String> javacOptions, File validationOutputDirectory) {
        this.checker = checker;
        this.javacOptions = new ArrayList<>(javacOptions);
        this.validator =
                new SimpleNninfRepairValidator(
                        checker, this.javacOptions, validationOutputDirectory);
    }

    public List<RepairDryRunResult> run(List<File> sourceFiles) {
        List<RepairDryRunResult> results = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            results.add(run(sourceFile));
        }
        return results;
    }

    public RepairDryRunResult run(File sourceFile) {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(checker, sourceFile, javacOptions);
        List<RepairDiagnostic> diagnostics = RepairDiagnosticAdapter.fromCaptureResult(capture);
        List<RepairDiagnostic> supportedDiagnostics = new ArrayList<>();
        List<RepairDiagnostic> unsupportedDiagnostics = new ArrayList<>();
        for (RepairDiagnostic diagnostic : diagnostics) {
            if (extractor.supports(diagnostic)) {
                supportedDiagnostics.add(diagnostic);
            } else {
                unsupportedDiagnostics.add(diagnostic);
            }
        }

        List<RepairConstraint> constraints = extractor.extract(supportedDiagnostics);
        UnsatCoreResult coreResult = solver.solve(constraints);
        List<RepairCandidate> candidates = planner.plan(coreResult);
        RepairBatchValidationResult validationResult =
                candidates.isEmpty() ? null : validator.validateAll(candidates);

        return new RepairDryRunResult(
                sourceFile,
                diagnostics,
                supportedDiagnostics,
                unsupportedDiagnostics,
                constraints,
                coreResult,
                candidates,
                validationResult);
    }
}
