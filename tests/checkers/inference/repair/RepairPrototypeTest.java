package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.CheckerDiagnosticCapture;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RepairPrototypeTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void extractsUnsatCoreForAssignmentFixture() {
        UnsatCoreResult result = solveFixture("testdata/repair/AssignmentRepair.java");

        assertFalse(result.isSatisfiable());
        assertEquals(1, result.getUnsatCore().size());
        assertEquals(
                "AssignmentRepair.java:8:assignment.type.incompatible:rhs_to_lhs",
                result.getUnsatCore().get(0).getLabel());
    }

    @Test
    public void extractsUnsatCoreForMethodCallFixture() {
        UnsatCoreResult result = solveFixture("testdata/repair/MethodCallRepair.java");

        assertFalse(result.isSatisfiable());
        assertEquals(1, result.getUnsatCore().size());
        assertEquals(
                "MethodCallRepair.java:8:argument.type.incompatible:arg_to_param",
                result.getUnsatCore().get(0).getLabel());
    }

    @Test
    public void validatesAssignmentRepairCandidate() {
        RepairValidationResult result =
                validateFirstCandidate("testdata/repair/AssignmentRepair.java");

        assertTrue(result.getCandidate().getDescription(), result.removesAllDiagnostics());
        assertEquals(0, result.getCheckerResult().getActualDiagnostics().size());
    }

    @Test
    public void validatesMethodCallRepairCandidate() {
        RepairValidationResult result =
                validateFirstCandidate("testdata/repair/MethodCallRepair.java");

        assertTrue(result.getCandidate().getDescription(), result.removesAllDiagnostics());
        assertEquals(0, result.getCheckerResult().getActualDiagnostics().size());
    }

    private static UnsatCoreResult solveFixture(String path) {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(NNINF_CHECKER, new File(path), NNINF_OPTIONS);
        List<RepairDiagnostic> diagnostics = RepairDiagnosticAdapter.fromCaptureResult(capture);
        List<RepairConstraint> constraints = new SimpleNninfConstraintExtractor().extract(diagnostics);
        return new SimpleNninfUnsatCoreSolver().solve(constraints);
    }

    private static RepairValidationResult validateFirstCandidate(String path) {
        UnsatCoreResult coreResult = solveFixture(path);
        List<RepairCandidate> candidates = new SimpleNninfRepairPlanner().plan(coreResult);
        assertEquals(1, candidates.size());
        return new SimpleNninfRepairValidator(
                        NNINF_CHECKER, NNINF_OPTIONS, new File("build/repair-prototype"))
                .validate(candidates.get(0));
    }
}
