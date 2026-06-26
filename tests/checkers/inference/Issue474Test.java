package checkers.inference;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import checkers.inference.solver.MaxSat2TypeSolver;
import checkers.inference.test.InferenceResult;
import checkers.inference.test.InferenceTestConfiguration;
import checkers.inference.test.InferenceTestConfigurationBuilder;
import checkers.inference.test.InferenceTestExecutor;

public class Issue474Test {

    @Test
    public void genericConditionalNestedClassDoesNotCrash() {
        File testFile = new File("testdata/inference/issue474/GenericConditionalNestedClass.java");
        InferenceTestConfiguration config =
                InferenceTestConfigurationBuilder.buildDefaultConfiguration(
                        "inference/issue474",
                        testFile,
                        new File("testdata"),
                        nninf.NninfChecker.class,
                        Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir"),
                        Collections.emptyList(),
                        MaxSat2TypeSolver.class.getCanonicalName(),
                        Collections.emptyList(),
                        true,
                        false,
                        System.getProperty("path.afu.scripts"),
                        System.getProperty("path.inference.script"));

        InferenceResult result = InferenceTestExecutor.infer(config);
        String output = result.getOutput();

        assertFalse(output, output.contains("The Checker Framework crashed"));
        assertFalse(output, output.contains("expensiveBackupGetPath"));
        assertFalse(output, output.contains("NullPointerException"));
    }
}
