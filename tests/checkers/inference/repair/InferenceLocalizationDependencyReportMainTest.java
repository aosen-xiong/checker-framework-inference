package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class InferenceLocalizationDependencyReportMainTest {
    @Test
    public void writesDependencyRowsForRunErrors() throws Exception {
        File csv = new File("build/inference-localization-dependency-report/input.csv");
        File output = new File("build/inference-localization-dependency-report/dependencies.csv");
        File parent = csv.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                csv.toPath(),
                ("projectName,sourceFile,runError,solverHadSolution\n"
                                + "\"eureka\",\"src/A.java\","
                                + "\"javac failed during inference: /tmp/A.java:1: error: "
                                + "cannot find symbol\\n  symbol:   class MissingType\\n\",false\n"
                                + "\"eureka\",\"src/B.java\",\"\",true\n")
                        .getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationDependencyReportMain.main(
                new String[] {"--csv", csv.getPath(), "--out", output.getPath()});

        List<String> lines = InferenceTestUtilities.getLines(output);
        assertEquals(2, lines.size());
        assertEquals("projectName,sourceFile,runErrorKind,missingDependencyExamples", lines.get(0));
        assertTrue(lines.get(1).contains("\"eureka\",\"src/A.java\",\"javac-missing-symbol\""));
        assertTrue(lines.get(1).contains("class MissingType"));
    }
}
