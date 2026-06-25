package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class InferenceRepairExperimentRunnerTest {
    @Test
    public void reportsSolvedInferenceInputWithoutRepairAttempts() {
        InferenceRepairExperimentResult result =
                runner("solved")
                        .run(new File("testdata/repair/AssignmentRepair.java"));

        assertFalse(result.hasRunError());
        assertTrue(result.initialInferenceSolved());
        assertFalse(result.repairSolvedInference());
        assertTrue(result.getCandidates().isEmpty());
        assertTrue(result.toJson().contains("\"initialInferenceSolved\":true"));
        assertTrue(result.toJson().contains("\"mode\":\"UNSAT_CORE_GUIDED\""));
        assertTrue(result.toJson().contains("\"editProviderMode\":\"DETERMINISTIC_ONLY\""));
        assertTrue(result.toJson().contains("\"candidateCount\":0"));
    }

    @Test
    public void reportsUnsatRepairSearchAndVerification() {
        InferenceRepairExperimentResult result =
                runner("unsat-assignment")
                        .run(new File("testdata/repair/InferenceUnsatAssignment.java"));

        assertFalse(result.hasRunError());
        assertFalse(result.initialInferenceSolved());
        assertTrue(result.getConstraintReport().getUnsatConstraintCount() > 0);
        assertTrue(result.getCandidates().size() > 0);
        assertTrue(result.validationResultCount() > 0);
        assertTrue(result.attemptCount() > 0);
        assertTrue(result.repairSolvedInference());
        assertTrue(result.repairFullyVerified());

        String json = result.toJson();
        assertTrue(json.contains("\"sourceFile\":\"testdata/repair/InferenceUnsatAssignment.java\""));
        assertTrue(json.contains("\"unsatConstraintCount\":"));
        assertTrue(json.contains("\"repairFullyVerified\":true"));
        assertTrue(json.contains("\"validationResults\":["));
        assertTrue(json.contains("\"attempts\":["));
    }

    @Test
    public void reportsBatchAggregates() {
        InferenceRepairExperimentBatchResult result =
                runner("batch")
                        .runAll(
                                Arrays.asList(
                                        new File("testdata/repair/AssignmentRepair.java"),
                                        new File(
                                                "testdata/repair/InferenceUnsatAssignment.java")));

        assertEquals(2, result.inputCount());
        assertEquals(0, result.runErrorCount());
        assertEquals(1, result.initialInferenceSolvedCount());
        assertEquals(1, result.repairSolvedInferenceCount());
        assertEquals(1, result.repairFullyVerifiedCount());
        assertTrue(result.candidateCount() > 0);
        assertTrue(result.attemptCount() > 0);

        String json = result.toJson();
        assertTrue(json.contains("\"inputCount\":2"));
        assertTrue(json.contains("\"mode\":\"UNSAT_CORE_GUIDED\""));
        assertTrue(json.contains("\"editProviderMode\":\"DETERMINISTIC_ONLY\""));
        assertTrue(json.contains("\"repairFullyVerifiedCount\":1"));
        assertTrue(json.contains("\"results\":["));
    }

    @Test
    public void reportsNoRepairAblation() {
        InferenceRepairExperimentResult result =
                runner(InferenceRepairExperimentConfig.noRepair(), "no-repair")
                        .run(new File("testdata/repair/InferenceUnsatAssignment.java"));

        assertFalse(result.hasRunError());
        assertFalse(result.initialInferenceSolved());
        assertFalse(result.repairSolvedInference());
        assertFalse(result.repairFullyVerified());
        assertEquals(0, result.getCandidates().size());
        assertEquals(0, result.validationResultCount());
        assertEquals(0, result.attemptCount());

        String json = result.toJson();
        assertTrue(json.contains("\"mode\":\"NO_REPAIR\""));
        assertTrue(json.contains("\"candidateCount\":0"));
        assertTrue(json.contains("\"repairFullyVerified\":false"));
    }

    @Test
    public void reportsUnsatCoreOnlyAblation() {
        InferenceRepairExperimentResult result =
                runner(InferenceRepairExperimentConfig.unsatCoreOnly(), "core-only")
                        .run(new File("testdata/repair/InferenceUnsatAssignment.java"));

        assertFalse(result.hasRunError());
        assertFalse(result.initialInferenceSolved());
        assertTrue(result.getCandidates().size() > 0);
        assertTrue(result.validationResultCount() > 0);
        assertTrue(result.toJson().contains("\"mode\":\"UNSAT_CORE_ONLY\""));
    }

    @Test
    public void reportsAiOnlyEditProviderAblation() {
        InferenceRepairExperimentResult result =
                runner(
                                InferenceRepairExperimentConfig.unsatCoreGuided()
                                        .withEditProviderMode(
                                                InferenceRepairEditProviderMode.AI_ONLY),
                                "ai-only",
                                new FixedAiRepairClient("fallbackId"))
                        .run(new File("testdata/repair/InferenceUnsatMethodCall.java"));

        assertFalse(result.hasRunError());
        assertTrue(result.repairFullyVerified());
        assertTrue(result.toJson().contains("\"editProviderMode\":\"AI_ONLY\""));
        assertTrue(result.toJson().contains("\"editOrigin\":\"AI\""));
    }

    @Test
    public void reportsDeterministicThenAiEditProviderAblation() {
        InferenceRepairExperimentResult result =
                runner(
                                InferenceRepairExperimentConfig.unsatCoreGuided()
                                        .withEditProviderMode(
                                                InferenceRepairEditProviderMode
                                                        .DETERMINISTIC_THEN_AI),
                                "deterministic-then-ai",
                                new FixedAiRepairClient("fallbackId"))
                        .run(new File("testdata/repair/InferenceUnsatMethodCall.java"));

        assertFalse(result.hasRunError());
        assertTrue(result.repairFullyVerified());
        assertTrue(result.toJson().contains("\"editProviderMode\":\"DETERMINISTIC_THEN_AI\""));
    }

    @Test
    public void rejectsAiModeWithoutAiClient() {
        InferenceRepairExperimentConfig config =
                InferenceRepairExperimentConfig.unsatCoreGuided()
                        .withEditProviderMode(InferenceRepairEditProviderMode.AI_ONLY);
        try {
            runner(config, "missing-ai-client", null);
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError("Expected AI mode to require an AI repair client.");
    }

    private static InferenceRepairExperimentRunner runner(String name) {
        return runner(InferenceRepairExperimentConfig.unsatCoreGuided(), name);
    }

    private static InferenceRepairExperimentRunner runner(
            InferenceRepairExperimentConfig experimentConfig, String name) {
        return new InferenceRepairExperimentRunner(
                experimentConfig,
                InferenceRepairConfiguration.nninfDefault(),
                new SimpleNninfRepairPlanner(),
                new File("build/inference-repair-experiments/" + name),
                new InferenceRepairEditGenerator());
    }

    private static InferenceRepairExperimentRunner runner(
            InferenceRepairExperimentConfig experimentConfig,
            String name,
            AiRepairClient aiRepairClient) {
        return new InferenceRepairExperimentRunner(
                experimentConfig,
                InferenceRepairConfiguration.nninfDefault(),
                new SimpleNninfRepairPlanner(),
                new File("build/inference-repair-experiments/" + name),
                aiRepairClient,
                Collections.<File>singletonList(
                        new File("testdata/repair/InferenceUnsatMethodCall.java")));
    }

    private static final class FixedAiRepairClient implements AiRepairClient {
        private final String replacementSource;

        private FixedAiRepairClient(String replacementSource) {
            this.replacementSource = replacementSource;
        }

        @Override
        public List<String> proposeReplacementSources(RepairPromptContext context) {
            return Collections.singletonList(replacementSource);
        }
    }
}
