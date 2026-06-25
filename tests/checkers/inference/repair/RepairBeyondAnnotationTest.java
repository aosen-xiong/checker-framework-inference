package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InferenceTestUtilities;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RepairBeyondAnnotationTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void repairsNullableDereferenceWithNullSafeExpression() {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER, new File("testdata/repair/NullDerefRepair.java"), NNINF_OPTIONS);
        List<RepairDiagnostic> diagnostics = RepairDiagnosticAdapter.fromCaptureResult(capture);

        SimpleNninfConstraintExtractor annotationExtractor = new SimpleNninfConstraintExtractor();
        assertEquals(1, diagnostics.size());
        assertFalse(annotationExtractor.supports(diagnostics.get(0)));

        List<CodeRepairCandidate> candidates = new SimpleNninfCodeRepairPlanner().plan(diagnostics);
        assertEquals(1, candidates.size());

        CodeRepairValidationResult result =
                new SimpleNninfCodeRepairValidator(
                                NNINF_CHECKER,
                                NNINF_OPTIONS,
                                new File("build/repair-beyond-annotation"))
                        .validate(candidates.get(0));

        assertTrue(
                result.getCandidate().getDescription()
                        + "\n"
                        + result.getCheckerResult().summarize(),
                result.removesAllDiagnostics());
        assertEquals(0, result.getCheckerResult().getActualDiagnostics().size());
    }

    @Test
    public void repairsNullableFieldReceiverDereferenceWithLocalGuard() {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/repair/NullableReceiverDerefRepair.java"),
                        NNINF_OPTIONS);
        List<RepairDiagnostic> diagnostics = RepairDiagnosticAdapter.fromCaptureResult(capture);

        assertEquals(1, diagnostics.size());

        List<CodeRepairCandidate> candidates = new SimpleNninfCodeRepairPlanner().plan(diagnostics);
        assertEquals(1, candidates.size());

        CodeRepairValidationResult result =
                new SimpleNninfCodeRepairValidator(
                                NNINF_CHECKER,
                                NNINF_OPTIONS,
                                new File("build/repair-beyond-annotation"))
                        .validate(candidates.get(0));

        assertTrue(
                result.getCandidate().getDescription()
                        + "\n"
                        + result.getCheckerResult().summarize(),
                result.removesAllDiagnostics());
        assertEquals(0, result.getCheckerResult().getActualDiagnostics().size());

        String repairedSource =
                String.join("\n", InferenceTestUtilities.getLines(result.getRepairedSourceFile()));
        assertTrue(repairedSource.contains("if (this.builder == null) {"));
        assertTrue(repairedSource.contains("@SuppressWarnings(\"cast.unsafe\")"));
        assertTrue(
                repairedSource.contains("Builder builder = (@nninf.qual.NonNull Builder) this.builder;"));
        assertTrue(repairedSource.contains("builder.add(name, value);"));
    }
}
