package checkers.inference.solver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.checkerframework.javacutil.UserError;
import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExternalSolverUtilsTest {

    @Test
    public void runExternalSolverHandlesOutputAndExitStatus() {
        assumeUnixShell();
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();

        int exitStatus =
                ExternalSolverUtils.runExternalSolver(
                        new String[] {
                            "/bin/sh", "-c", "printf 'out\\n'; printf 'err\\n' 1>&2; exit 7"
                        },
                        reader -> readLines(reader, stdout),
                        reader -> readLines(reader, stderr));

        assertEquals(7, exitStatus);
        assertEquals("out", stdout.get(0));
        assertEquals("err", stderr.get(0));
    }

    @Test
    public void runExternalSolverReportsCommandStartFailure() {
        String missingCommand = "missing-external-solver-command-for-test";

        try {
            ExternalSolverUtils.runExternalSolver(
                    new String[] {missingCommand}, reader -> {}, reader -> {});
        } catch (UserError e) {
            assertTrue(e.getMessage(), e.getMessage().contains(missingCommand));
            return;
        }

        throw new AssertionError("Expected UserError for missing external solver command.");
    }

    private static void readLines(BufferedReader reader, List<String> lines) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assumeUnixShell() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));
    }
}
