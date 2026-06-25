package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceLocalizationMatrixFileMainTest {
    @Test
    public void writesMatrixFromSourceListDirectory() throws Exception {
        File sourceListDirectory = new File("build/inference-localization-matrix-file/lists");
        File benchmarkRoot = new File("build/inference-localization-matrix-file/benchmarks");
        File outputFile = new File("build/inference-localization-matrix-file/matrix.csv");
        if (!sourceListDirectory.exists() && !sourceListDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create " + sourceListDirectory);
        }
        Files.write(
                new File(sourceListDirectory, "retrofit.txt").toPath(),
                "src/main/java/A.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                new File(sourceListDirectory, "conductor.txt").toPath(),
                "src/main/java/B.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                new File(sourceListDirectory, "notes.md").toPath(),
                "ignored\n".getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationMatrixFileMain.main(
                new String[] {
                    "--source-list-dir",
                    sourceListDirectory.getPath(),
                    "--benchmark-root",
                    benchmarkRoot.getPath(),
                    "--out",
                    outputFile.getPath()
                });

        String matrix = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(matrix.contains("# projectName,sourceList,projectRoot"));
        assertTrue(matrix.contains("\"conductor\""));
        assertTrue(matrix.contains("\"retrofit\""));
        assertTrue(matrix.contains("\"" + new File(benchmarkRoot, "retrofit").getPath() + "\""));
        assertFalse(matrix.contains("notes.md"));
    }

    @Test
    public void filtersMatrixProjectsByIncludeAndExcludeLists() throws Exception {
        File sourceListDirectory = new File("build/inference-localization-matrix-file-filtered/lists");
        File outputFile = new File("build/inference-localization-matrix-file-filtered/matrix.csv");
        if (!sourceListDirectory.exists() && !sourceListDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create " + sourceListDirectory);
        }
        Files.write(
                new File(sourceListDirectory, "retrofit.txt").toPath(),
                "src/main/java/A.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                new File(sourceListDirectory, "conductor.txt").toPath(),
                "src/main/java/B.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                new File(sourceListDirectory, "repair-fixtures.txt").toPath(),
                "testdata/repair/A.java\n".getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationMatrixFileMain.main(
                new String[] {
                    "--source-list-dir",
                    sourceListDirectory.getPath(),
                    "--include-projects",
                    "retrofit,conductor,missing",
                    "--exclude-projects",
                    "conductor",
                    "--out",
                    outputFile.getPath()
                });

        String matrix = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(matrix.contains("\"retrofit\""));
        assertFalse(matrix.contains("\"conductor\""));
        assertFalse(matrix.contains("\"repair-fixtures\""));
    }

    @Test
    public void writesOptionalCompanionSourceListColumn() throws Exception {
        File sourceListDirectory = new File("build/inference-localization-matrix-file-companion/lists");
        File companionSourceListDirectory =
                new File("build/inference-localization-matrix-file-companion/companions");
        File benchmarkRoot = new File("build/inference-localization-matrix-file-companion/benchmarks");
        File outputFile = new File("build/inference-localization-matrix-file-companion/matrix.csv");
        if (!sourceListDirectory.exists() && !sourceListDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create " + sourceListDirectory);
        }
        if (!companionSourceListDirectory.exists() && !companionSourceListDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create " + companionSourceListDirectory);
        }
        Files.write(
                new File(sourceListDirectory, "eureka.txt").toPath(),
                "src/main/java/A.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                new File(companionSourceListDirectory, "eureka.txt").toPath(),
                "src/main/java/A.java\nsrc/main/java/B.java\n".getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationMatrixFileMain.main(
                new String[] {
                    "--source-list-dir",
                    sourceListDirectory.getPath(),
                    "--benchmark-root",
                    benchmarkRoot.getPath(),
                    "--companion-source-list-dir",
                    companionSourceListDirectory.getPath(),
                    "--out",
                    outputFile.getPath()
                });

        String matrix = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(matrix.contains("# projectName,sourceList,projectRoot[,companionSourceList]"));
        assertTrue(matrix.contains("\"" + new File(companionSourceListDirectory, "eureka.txt").getPath() + "\""));
    }
}
