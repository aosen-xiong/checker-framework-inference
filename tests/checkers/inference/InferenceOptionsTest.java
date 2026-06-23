package checkers.inference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import checkers.inference.solver.MaxSat2TypeSolver;

public class InferenceOptionsTest {

    @Before
    public void resetOptions() {
        InferenceOptions.mode = null;
        InferenceOptions.hacks = false;
        InferenceOptions.typesystem = null;
        InferenceOptions.jaifFile = InferenceOptions.DEFAULT_JAIF;
        InferenceOptions.checker = null;
        InferenceOptions.solver = null;
        InferenceOptions.targetclasspath = ".";
        InferenceOptions.solverArgs = null;
        InferenceOptions.cfArgs = null;
        InferenceOptions.jsonFile = null;
        InferenceOptions.pathToAfuScripts = null;
        InferenceOptions.afuOutputDir = null;
        InferenceOptions.inPlace = false;
        InferenceOptions.afuOptions = null;
        InferenceOptions.makeDefaultsExplicit = false;
        InferenceOptions.version = false;
        InferenceOptions.help = false;
        InferenceOptions.logLevel = null;
        InferenceOptions.printCommands = false;
        InferenceOptions.debug = null;
        InferenceOptions.javacOptions = new ArrayList<String>();
        InferenceOptions.javaFiles = new String[0];
    }

    @Test
    public void acceptsAfuOptionsWithoutOutputDirectory() {
        InferenceOptions.InitStatus status = initRoundtripWithAfuOptions("--debug -Dkey=value");

        assertTrue(status.errors.toString(), status.errors.isEmpty());
        assertEquals("ROUNDTRIP", InferenceOptions.mode);
    }

    @Test
    public void rejectsShortAfuOutputDirectoryOption() {
        InferenceOptions.InitStatus status = initRoundtripWithAfuOptions("-d generated");

        assertContainsAfuOutputDirectoryError(status.errors);
    }

    @Test
    public void rejectsLongAfuOutdirOption() {
        InferenceOptions.InitStatus status = initRoundtripWithAfuOptions("--outdir=generated");

        assertContainsAfuOutputDirectoryError(status.errors);
    }

    @Test
    public void rejectsLongAfuDirectoryOption() {
        InferenceOptions.InitStatus status = initRoundtripWithAfuOptions("--directory generated");

        assertContainsAfuOutputDirectoryError(status.errors);
    }

    @Test
    public void ignoresOutputDirectoryOptionSubstrings() {
        assertFalse(InferenceOptions.containsAfuOutputDirectoryOption("--debug output-d-files"));
        assertFalse(InferenceOptions.containsAfuOutputDirectoryOption("-debug"));
        assertFalse(InferenceOptions.containsAfuOutputDirectoryOption("--outdirish=generated"));
    }

    @Test
    public void validateThrowsForMissingRequiredMode() {
        InferenceOptions.InitStatus status =
                InferenceOptions.init(
                        new String[] {"--checker", ostrusted.OsTrustedChecker.class.getName()},
                        true);

        InferenceOptions.InvalidOptionsException exception = expectInvalidOptions(status);
        assertTrue(exception.getMessage(), exception.getMessage().contains("mode of operation"));
    }

    @Test
    public void validateThrowsForInvalidModeWithoutExiting() {
        InferenceOptions.InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--mode=NOT_A_MODE",
                            "--checker",
                            ostrusted.OsTrustedChecker.class.getName()
                        },
                        true);

        InferenceOptions.InvalidOptionsException exception = expectInvalidOptions(status);
        assertTrue(
                exception.getMessage(), exception.getMessage().contains("Could not recognize mode"));
    }

    @Test
    public void validateThrowsForHelpRequestWithoutExiting() {
        InferenceOptions.InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--help",
                            "--mode=TYPECHECK",
                            "--checker",
                            ostrusted.OsTrustedChecker.class.getName()
                        },
                        true);

        try {
            status.validate();
        } catch (InferenceOptions.HelpRequestedException e) {
            assertEquals(status, e.getStatus());
            return;
        }

        throw new AssertionError("Expected HelpRequestedException.");
    }

    private static InferenceOptions.InitStatus initRoundtripWithAfuOptions(String afuOptions) {
        return InferenceOptions.init(
                new String[] {
                    "--mode=ROUNDTRIP",
                    "--checker",
                    ostrusted.OsTrustedChecker.class.getName(),
                    "--solver",
                    MaxSat2TypeSolver.class.getName(),
                    "--inPlace",
                    "--afuOptions=" + afuOptions
                },
                true);
    }

    private static void assertContainsAfuOutputDirectoryError(List<String> errors) {
        assertEquals(errors.toString(), 1, errors.size());
        assertTrue(
                errors.toString(),
                errors.get(0).contains("Annotation File Utilities output dir"));
    }

    private static InferenceOptions.InvalidOptionsException expectInvalidOptions(
            InferenceOptions.InitStatus status) {
        try {
            status.validate();
        } catch (InferenceOptions.InvalidOptionsException e) {
            assertEquals(status, e.getStatus());
            return e;
        }

        throw new AssertionError("Expected InvalidOptionsException.");
    }
}
