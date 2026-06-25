package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceLocalizationStudySummaryMainTest {
    @Test
    public void writesMarkdownSummaryForCsvReport() throws Exception {
        File csv = new File("build/inference-localization-study-summary/input.csv");
        File output = new File("build/inference-localization-study-summary/summary.md");
        File parent = csv.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                csv.toPath(),
                ("runError,solverHadSolution,mcsOracleKind,mcsSearchBounded,"
                                + "mcsUniverseTruncated,hasSourceRealizableRepairUnit,"
                                + "top1HasSourceRealizableRepairUnit,top1Locations,"
                                + "sourceRepairPlanMaterialized,"
                                + "sourceRepairPlanInferenceSolved,sourceRepairPlanVerified,"
                                + "sourceRepairFollowUpAttempted,sourceRepairFollowUpVerified,"
                                + "sourceRepairFollowUpAppliedEdit\n"
                                + "\"\",true,\"SOLVER_BACKED\",false,false,false,false,\"\","
                                + "false,false,false,false,false,\"\"\n"
                                + "\"\",false,\"SOLVER_BACKED\",false,false,true,true,\"loc\","
                                + "true,true,true,true,true,\"edit\"\n"
                                + "\"TIMEOUT after 20 seconds\",false,\"NONE\",false,false,false,false,\"\","
                                + "false,false,false,false,false,\"\"\n")
                        .getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationStudySummaryMain.main(
                new String[] {"--csv", csv.getPath(), "--out", output.getPath()});

        String summary = String.join("\n", InferenceTestUtilities.getLines(output));
        assertTrue(summary.contains("Total cases: 3"));
        assertTrue(summary.contains("SAT cases: 1"));
        assertTrue(summary.contains("UNSAT cases: 1"));
        assertTrue(summary.contains("Timeouts: 1"));
        assertTrue(summary.contains("Rows with source-realizable repair units: 1"));
        assertTrue(summary.contains("UNSAT rows with source-realizable repair units: 1"));
        assertTrue(summary.contains("UNSAT rows with source-realizable top-1 MCS candidates: 1"));
        assertTrue(summary.contains("Source repair plans materialized: 1"));
        assertTrue(summary.contains("Source repair plan validations attempted: 1"));
        assertTrue(summary.contains("Validation attempts that solved inference: 1"));
        assertTrue(summary.contains("Validation attempts fully verified: 1"));
        assertTrue(summary.contains("Residual diagnostic repairs attempted: 1"));
        assertTrue(summary.contains("Residual diagnostic repairs verified: 1"));
        assertTrue(summary.contains("UNSAT yield: 33.3%"));
        assertTrue(summary.contains("Candidate yield among UNSAT: 100.0%"));
        assertTrue(summary.contains("Source-realizable top-1 candidate yield among UNSAT: 100.0%"));
        assertTrue(summary.contains("Source-realizable yield among UNSAT: 100.0%"));
        assertTrue(summary.contains("Materialization yield among UNSAT: 100.0%"));
        assertTrue(summary.contains("Repair validation attempt yield among UNSAT: 100.0%"));
        assertTrue(summary.contains("Inference-solved yield among validation attempts: 100.0%"));
        assertTrue(summary.contains("Full verification yield among validation attempts: 100.0%"));
        assertTrue(summary.contains("Residual diagnostic repair verification yield: 100.0%"));
    }

    @Test
    public void classifiesRunErrorsForBenchmarkTriage() throws Exception {
        File csv = new File("build/inference-localization-study-summary/run-errors.csv");
        File output = new File("build/inference-localization-study-summary/run-errors-summary.md");
        File parent = csv.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                csv.toPath(),
                ("runError,solverHadSolution,mcsOracleKind,mcsSearchBounded,"
                                + "mcsUniverseTruncated,hasSourceRealizableRepairUnit,"
                                + "top1HasSourceRealizableRepairUnit,top1Locations,"
                                + "sourceRepairPlanMaterialized,"
                                + "sourceRepairPlanInferenceSolved,sourceRepairPlanVerified,"
                                + "sourceRepairFollowUpAttempted,sourceRepairFollowUpVerified,"
                                + "sourceRepairFollowUpAppliedEdit\n"
                                + "\"javac failed during inference: /tmp/Test.java:4: error: "
                                + "cannot find symbol\\n  symbol:   class MissingType\\n\","
                                + "false,\"NONE\",false,false,false,false,\"\","
                                + "false,false,false,false,false,\"\"\n"
                                + "\"javac failed during inference: /tmp/Test.java:2: error: "
                                + "package com.example.missing does not exist\\n\","
                                + "false,\"NONE\",false,false,false,false,\"\","
                                + "false,false,false,false,false,\"\"\n")
                        .getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationStudySummaryMain.main(
                new String[] {"--csv", csv.getPath(), "--out", output.getPath()});

        String summary = String.join("\n", InferenceTestUtilities.getLines(output));
        assertTrue(summary.contains("Run errors: 2"));
        assertTrue(summary.contains("javac-missing-symbol: 1"));
        assertTrue(summary.contains("javac-missing-package: 1"));
        assertTrue(summary.contains("`class MissingType`"));
        assertTrue(summary.contains("`package com.example.missing`"));
    }
}
