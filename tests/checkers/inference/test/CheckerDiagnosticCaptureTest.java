package checkers.inference.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class CheckerDiagnosticCaptureTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void capturesCleanCheckerRun() {
        CheckerDiagnosticCapture.Result result =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/nninf/SimpleNninfTest1.java"),
                        NNINF_OPTIONS);

        printDiagnostics("clean nninf", result);
        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(0, result.getExpectedDiagnostics().size());
        assertEquals(0, result.getActualDiagnostics().size());
    }

    @Test
    public void capturesFixableCheckerDiagnostics() {
        CheckerDiagnosticCapture.Result result =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/nninf/FixableError1.java"),
                        NNINF_OPTIONS);

        printDiagnostics("fixable nninf", result);
        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(2, result.getExpectedDiagnostics().size());
        assertEquals(2, result.getActualDiagnostics().size());
    }

    @Test
    public void capturesRepairAssignmentFixtureDiagnostics() {
        CheckerDiagnosticCapture.Result result =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/repair/AssignmentRepair.java"),
                        NNINF_OPTIONS);

        printDiagnostics("repair assignment", result);
        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(1, result.getExpectedDiagnostics().size());
        assertEquals(1, result.getActualDiagnostics().size());
    }

    @Test
    public void capturesRepairMethodCallFixtureDiagnostics() {
        CheckerDiagnosticCapture.Result result =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/repair/MethodCallRepair.java"),
                        NNINF_OPTIONS);

        printDiagnostics("repair method call", result);
        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(1, result.getExpectedDiagnostics().size());
        assertEquals(1, result.getActualDiagnostics().size());
    }

    @Test
    public void capturesRepairNullDerefFixtureDiagnostics() {
        CheckerDiagnosticCapture.Result result =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/repair/NullDerefRepair.java"),
                        NNINF_OPTIONS);

        printDiagnostics("repair null dereference", result);
        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(1, result.getExpectedDiagnostics().size());
        assertEquals(1, result.getActualDiagnostics().size());
    }

    private static void printDiagnostics(String label, CheckerDiagnosticCapture.Result result) {
        System.out.println("=== " + label + " ===");
        System.out.println("checker: " + result.getChecker().getCanonicalName());
        System.out.println("sources: " + result.getSourceFiles());
        System.out.println("javac options: " + result.getJavacOptions());
        System.out.println("expected diagnostics:");
        if (result.getExpectedDiagnostics().isEmpty()) {
            System.out.println("  <none>");
        }
        for (TestDiagnostic diagnostic : result.getExpectedDiagnostics()) {
            System.out.println("  " + diagnostic.repr());
        }
        System.out.println("actual diagnostics:");
        if (result.getActualDiagnostics().isEmpty()) {
            System.out.println("  <none>");
        }
        for (Diagnostic<? extends JavaFileObject> diagnostic : result.getActualDiagnostics()) {
            System.out.println(
                    "  "
                            + diagnostic.getKind()
                            + " "
                            + diagnostic.getSource().getName()
                            + ":"
                            + diagnostic.getLineNumber()
                            + ": "
                            + diagnostic.getMessage(null));
        }
    }
}
