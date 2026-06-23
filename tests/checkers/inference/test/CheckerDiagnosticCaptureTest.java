package checkers.inference.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

        assertFalse(result.summarize(), result.didTestFail());
        assertEquals(2, result.getExpectedDiagnostics().size());
        assertEquals(2, result.getActualDiagnostics().size());
    }
}
