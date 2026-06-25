package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class InferenceRepairExperimentCompareMainTest {
    @Test
    public void writesCsvRowsForReports() throws Exception {
        File root = new File("build/inference-repair-experiment-compare/reports");
        File first = new File(root, "first.json");
        File second = new File(root, "second.json");
        File output = new File(root, "comparison.csv");
        writeReport(first, "fixture-a", "r1", "NO_REPAIR", 2, 1);
        writeReport(second, "fixture-b", "r2", "UNSAT_CORE_GUIDED", 3, 0);

        InferenceRepairExperimentCompareMain.main(
                new String[] {
                    "--out", output.getPath(), "--report", first.getPath(), "--report", second.getPath()
                });

        List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals(InferenceRepairExperimentSummary.csvHeader(), lines.get(0));
        assertTrue(lines.get(1).contains("\"fixture-a\",\"r1\",\"NO_REPAIR\""));
        assertTrue(lines.get(1).endsWith(",2,0,1,0,0,4,5"));
        assertTrue(lines.get(2).contains("\"fixture-b\",\"r2\",\"UNSAT_CORE_GUIDED\""));
        assertTrue(lines.get(2).endsWith(",3,0,0,0,0,4,5"));
    }

    @Test
    public void acceptsReportListFile() throws Exception {
        File root = new File("build/inference-repair-experiment-compare/report-list");
        File report = new File(root, "report.json");
        File list = new File(root, "reports.txt");
        File output = new File(root, "comparison.csv");
        writeReport(report, "fixture-list", "r3", "UNSAT_CORE_ONLY", 1, 0);
        writeFile(list, "# comment\n\n" + report.getPath() + "\n");

        InferenceRepairExperimentCompareMain.main(
                new String[] {"--out", output.getPath(), "--report-list", list.getPath()});

        String csv = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"fixture-list\",\"r3\",\"UNSAT_CORE_ONLY\""));
    }

    @Test
    public void parsesFormattedJsonAndEscapedStrings() throws Exception {
        File root = new File("build/inference-repair-experiment-compare/formatted");
        File report = new File(root, "formatted.json");
        File output = new File(root, "comparison.csv");
        writeFile(
                report,
                "{\n"
                        + "  \"projectName\": \"fixture \\\"quoted\\\"\",\n"
                        + "  \"projectRoot\": \".\",\n"
                        + "  \"revision\": \"r4\",\n"
                        + "  \"mode\": \"NO_REPAIR\",\n"
                        + "  \"editProviderMode\": \"DETERMINISTIC_ONLY\",\n"
                        + "  \"inputCount\": 1,\n"
                        + "  \"runErrorCount\": 0,\n"
                        + "  \"initialInferenceSolvedCount\": 1,\n"
                        + "  \"repairSolvedInferenceCount\": 0,\n"
                        + "  \"repairFullyVerifiedCount\": 0,\n"
                        + "  \"candidateCount\": 0,\n"
                        + "  \"attemptCount\": 0,\n"
                        + "  \"results\": []\n"
                        + "}\n");

        InferenceRepairExperimentCompareMain.main(
                new String[] {"--out", output.getPath(), "--report", report.getPath()});

        String csv = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"fixture \"\"quoted\"\"\",\"r4\",\"NO_REPAIR\""));
    }

    private static void writeReport(
            File file,
            String projectName,
            String revision,
            String mode,
            int inputCount,
            int initialInferenceSolvedCount)
            throws Exception {
        String json =
                "{"
                        + "\"projectName\":\""
                        + projectName
                        + "\","
                        + "\"projectRoot\":\".\","
                        + "\"revision\":\""
                        + revision
                        + "\","
                        + "\"mode\":\""
                        + mode
                        + "\","
                        + "\"editProviderMode\":\"DETERMINISTIC_ONLY\","
                        + "\"inputCount\":"
                        + inputCount
                        + ","
                        + "\"runErrorCount\":0,"
                        + "\"initialInferenceSolvedCount\":"
                        + initialInferenceSolvedCount
                        + ","
                        + "\"repairSolvedInferenceCount\":0,"
                        + "\"repairFullyVerifiedCount\":0,"
                        + "\"candidateCount\":4,"
                        + "\"attemptCount\":5,"
                        + "\"results\":[]"
                        + "}";
        writeFile(file, json);
    }

    private static void writeFile(File file, String contents) throws Exception {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
