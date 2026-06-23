package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dry-run repair report for one source file. */
public final class RepairDryRunResult {
    private final File sourceFile;
    private final List<RepairDiagnostic> diagnostics;
    private final List<RepairDiagnostic> supportedDiagnostics;
    private final List<RepairDiagnostic> unsupportedDiagnostics;
    private final List<RepairConstraint> constraints;
    private final UnsatCoreResult coreResult;
    private final List<RepairCandidate> candidates;
    private final RepairBatchValidationResult validationResult;

    public RepairDryRunResult(
            File sourceFile,
            List<RepairDiagnostic> diagnostics,
            List<RepairDiagnostic> supportedDiagnostics,
            List<RepairDiagnostic> unsupportedDiagnostics,
            List<RepairConstraint> constraints,
            UnsatCoreResult coreResult,
            List<RepairCandidate> candidates,
            RepairBatchValidationResult validationResult) {
        this.sourceFile = sourceFile;
        this.diagnostics = immutableCopy(diagnostics);
        this.supportedDiagnostics = immutableCopy(supportedDiagnostics);
        this.unsupportedDiagnostics = immutableCopy(unsupportedDiagnostics);
        this.constraints = immutableCopy(constraints);
        this.coreResult = coreResult;
        this.candidates = immutableCopy(candidates);
        this.validationResult = validationResult;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public List<RepairDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public List<RepairDiagnostic> getSupportedDiagnostics() {
        return supportedDiagnostics;
    }

    public List<RepairDiagnostic> getUnsupportedDiagnostics() {
        return unsupportedDiagnostics;
    }

    public List<RepairConstraint> getConstraints() {
        return constraints;
    }

    public UnsatCoreResult getCoreResult() {
        return coreResult;
    }

    public List<RepairCandidate> getCandidates() {
        return candidates;
    }

    public RepairBatchValidationResult getValidationResult() {
        return validationResult;
    }

    public boolean validated() {
        return validationResult != null && validationResult.removesAllDiagnostics();
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
