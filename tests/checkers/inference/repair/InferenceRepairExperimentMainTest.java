package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceRepairExperimentMainTest {
    @Test
    public void writesJsonReportForSourceInputs() throws Exception {
        File outputFile = new File("build/inference-repair-experiment-main/report.json");

        InferenceRepairExperimentMain.main(
                new String[] {
                    "--mode",
                    "NO_REPAIR",
                    "--project-name",
                    "fixture-project",
                    "--project-root",
                    ".",
                    "--revision",
                    "test-revision",
                    "--out",
                    outputFile.getPath(),
                    "--source",
                    "testdata/repair/InferenceUnsatAssignment.java"
                });

        assertTrue(outputFile.isFile());
        String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"mode\":\"NO_REPAIR\""));
        assertTrue(json.contains("\"projectName\":\"fixture-project\""));
        assertTrue(json.contains("\"revision\":\"test-revision\""));
        assertTrue(json.contains("\"inputCount\":1"));
        assertTrue(json.contains("\"sourceFile\":\"testdata/repair/InferenceUnsatAssignment.java\""));
    }

    @Test
    public void writesJsonReportForSourceList() throws Exception {
        File sourceList = new File("build/inference-repair-experiment-main/sources.txt");
        File outputFile = new File("build/inference-repair-experiment-main/source-list-report.json");
        File parent = sourceList.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                sourceList.toPath(),
                ("testdata/repair/AssignmentRepair.java\n"
                                + "testdata/repair/InferenceUnsatAssignment.java\n")
                        .getBytes(StandardCharsets.UTF_8));

        InferenceRepairExperimentMain.main(
                new String[] {
                    "--mode", "NO_REPAIR", "--out", outputFile.getPath(), "--source-list", sourceList.getPath()
                });

        String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"inputCount\":2"));
        assertTrue(json.contains("\"initialInferenceSolvedCount\":1"));
    }

    @Test
    public void resolvesSourceListEntriesAgainstProjectRootRegardlessOfArgumentOrder()
            throws Exception {
        File sourceList = new File("build/inference-repair-experiment-main/relative-sources.txt");
        File outputFile =
                new File("build/inference-repair-experiment-main/relative-source-list-report.json");
        File parent = sourceList.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                sourceList.toPath(),
                "testdata/repair/InferenceUnsatAssignment.java\n".getBytes(StandardCharsets.UTF_8));

        InferenceRepairExperimentMain.main(
                new String[] {
                    "--mode",
                    "NO_REPAIR",
                    "--out",
                    outputFile.getPath(),
                    "--source-list",
                    sourceList.getPath(),
                    "--project-root",
                    "."
                });

        String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"projectRoot\":\".\""));
        assertTrue(json.contains("\"sourceFile\":\"./testdata/repair/InferenceUnsatAssignment.java\""));
    }

    @Test
    public void writesJsonReportForSourceRootDiscovery() throws Exception {
        File outputFile =
                new File("build/inference-repair-experiment-main/source-root-report.json");

        InferenceRepairExperimentMain.main(
                new String[] {
                    "--mode",
                    "NO_REPAIR",
                    "--out",
                    outputFile.getPath(),
                    "--source-root",
                    "testdata/repair"
                });

        String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"inputCount\":"));
        assertTrue(json.contains("testdata/repair/InferenceUnsatAssignment.java"));
        assertTrue(json.contains("testdata/repair/InferenceUnsatMethodCall.java"));
    }
}
