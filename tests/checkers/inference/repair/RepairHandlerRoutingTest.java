package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.CheckerDiagnosticCapture;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RepairHandlerRoutingTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void routesSubtypeDiagnosticsToAnnotationHandler() {
        List<RepairHandlerResult> results =
                router("build/repair-handler-routing/assignment")
                        .repair(captureDiagnostics("testdata/repair/AssignmentRepair.java"));

        assertEquals(1, results.size());
        assertEquals("subtype-annotation", results.get(0).getHandlerName());
        assertTrue(results.get(0).removesAllDiagnostics());
    }

    @Test
    public void routesNullableDereferenceToCodeHandler() {
        List<RepairHandlerResult> results =
                router("build/repair-handler-routing/null-deref")
                        .repair(captureDiagnostics("testdata/repair/NullDerefRepair.java"));

        assertEquals(1, results.size());
        assertEquals("nullable-dereference", results.get(0).getHandlerName());
        assertTrue(results.get(0).removesAllDiagnostics());
    }

    @Test
    public void routesMixedDiagnosticsToMultipleHandlers() {
        List<RepairDiagnostic> diagnostics =
                captureDiagnostics("testdata/repair/MethodCallRepair.java");
        diagnostics.addAll(captureDiagnostics("testdata/repair/NullDerefRepair.java"));

        List<RepairHandlerResult> results =
                router("build/repair-handler-routing/mixed").repair(diagnostics);

        assertEquals(2, results.size());
        assertEquals("subtype-annotation", results.get(0).getHandlerName());
        assertEquals("nullable-dereference", results.get(1).getHandlerName());
        assertTrue(results.get(0).removesAllDiagnostics());
        assertTrue(results.get(1).removesAllDiagnostics());
    }

    private static RepairRouter router(String outputDirectory) {
        return new RepairRouter(
                Arrays.asList(
                        new SubtypeAnnotationRepairHandler(
                                NNINF_CHECKER,
                                NNINF_OPTIONS,
                                new File(outputDirectory, "annotation")),
                        new NullableDereferenceRepairHandler(
                                NNINF_CHECKER, NNINF_OPTIONS, new File(outputDirectory, "code"))));
    }

    private static List<RepairDiagnostic> captureDiagnostics(String path) {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(NNINF_CHECKER, new File(path), NNINF_OPTIONS);
        return RepairDiagnosticAdapter.fromCaptureResult(capture);
    }
}
