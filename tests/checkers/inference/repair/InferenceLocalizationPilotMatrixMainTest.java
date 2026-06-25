package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceLocalizationPilotMatrixMainTest {
    @Test
    public void runsPilotMatrixForSourceListProjects() throws Exception {
        File root = new File(".");
        File sourceList = new File("build/inference-localization-pilot-matrix/sources.txt");
        File matrix = new File("build/inference-localization-pilot-matrix/matrix.csv");
        File outputDirectory = new File("build/inference-localization-pilot-matrix/out");
        File parent = sourceList.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                sourceList.toPath(),
                "testdata/repair/InferenceUnsatAssignment.java\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                matrix.toPath(),
                ("fixture," + sourceList.getPath() + "," + root.getPath() + "\n")
                        .getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationPilotMatrixMain.main(
                new String[] {
                    "--matrix",
                    matrix.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--sample-size",
                    "1",
                    "--top-k",
                    "2",
                    "--timeout-seconds",
                    "20",
                    "--validate-source-repair-plans"
                });

        File json = new File(outputDirectory, "fixture.json");
        File csv = new File(outputDirectory, "fixture.csv");
        File summary = new File(outputDirectory, "fixture-summary.md");
        File matrixSummary = new File(outputDirectory, "matrix-summary.csv");
        assertTrue(json.isFile());
        assertTrue(csv.isFile());
        assertTrue(summary.isFile());
        assertTrue(matrixSummary.isFile());
        assertTrue(
                String.join("\n", InferenceTestUtilities.getLines(summary))
                        .contains("Total cases: 1"));
        String aggregate = String.join("\n", InferenceTestUtilities.getLines(matrixSummary));
        assertTrue(
                aggregate.contains(
                        "project,total,sat,unsat,runErrors,timeouts,unsatYield,timeoutRate,"
                                + "candidateYield,sourceRealizableCandidateYield,"
                                + "sourceRealizableYield,repairPlanMaterialized,"
                                + "repairPlanAttempted,repairPlanInferenceSolved,"
                                + "repairPlanVerified,"
                                + "repairFollowUpAttempted,repairFollowUpVerified,"
                                + "repairPlanMaterializationYield,repairPlanAttemptYield,"
                                + "repairPlanInferenceSolvedYield,"
                                + "repairPlanVerifiedYield,repairFollowUpVerifiedYield"));
        assertTrue(aggregate.contains("\"fixture\""));
        assertTrue(aggregate.contains("\"100.0%\""));
        assertTrue(aggregate.contains("\"0.0%\""));
    }
}
