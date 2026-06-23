package checkers.inference.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import checkers.inference.solver.MaxSat2TypeSolver;

public class InferenceTestConfigurationBuilderTest {

    @Test
    public void defaultOutputDirectoryIsUnderBuild() {
        InferenceTestConfiguration config = buildTestConfiguration();

        String outputPath = config.getOutputJaif().getPath();
        assertTrue(outputPath, outputPath.startsWith("build" + File.separator + "inference-tests"));
        assertFalse(outputPath, outputPath.contains("testdata" + File.separator + "tmp"));
    }

    @Test
    public void prepareInferenceOutputDirectoryDeletesStaleFiles() throws IOException {
        InferenceTestConfiguration config = buildTestConfiguration();
        File outputDir = config.getOutputJaif().getParentFile();
        File staleFile = new File(outputDir, "stale.txt");

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create " + outputDir.getAbsolutePath());
        }
        if (!staleFile.createNewFile()) {
            throw new IOException("Could not create " + staleFile.getAbsolutePath());
        }

        InferenceTestExecutor.prepareInferenceOutputDirectory(config);

        assertFalse(staleFile.exists());
        assertTrue(config.getAnnotatedSourceDir().isDirectory());
        assertTrue(config.getOutputJaif().getParentFile().isDirectory());
    }

    private static InferenceTestConfiguration buildTestConfiguration() {
        return InferenceTestConfigurationBuilder.buildDefaultConfiguration(
                "ostrusted",
                new File("testdata/ostrusted/Test.java"),
                new File("testdata"),
                ostrusted.OsTrustedChecker.class,
                Arrays.asList("-d", "tests/build/outputdir"),
                Collections.<String>emptyList(),
                MaxSat2TypeSolver.class.getName(),
                Collections.<String>emptyList(),
                true,
                false,
                "",
                "./scripts/inference");
    }
}
