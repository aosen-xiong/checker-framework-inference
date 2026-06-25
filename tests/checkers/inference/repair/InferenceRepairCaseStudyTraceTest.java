package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceRepairCaseStudyTraceTest {
    @Test
    public void rendersMethodCallRepairCaseStudy() throws Exception {
        File source = new File("testdata/repair/InferenceUnsatMethodCall.java");
        InferenceRepairExperimentResult result =
                new InferenceRepairExperimentRunner(
                                InferenceRepairExperimentConfig.unsatCoreGuided(),
                                InferenceRepairConfiguration.nninfDefault(),
                                new SimpleNninfRepairPlanner(),
                                new File("build/inference-repair-case-study-test/work"),
                                new InferenceRepairEditGenerator())
                        .run(source);

        String trace = new InferenceRepairCaseStudyTrace().render(result);

        assertTrue(trace.contains("# Inference Repair Case Study"));
        assertTrue(trace.contains("Solver had solution: `false`"));
        assertTrue(trace.contains("Repair Candidates"));
        assertTrue(trace.contains("Target: `IDENTIFIER"));
        assertTrue(trace.contains("recordId(maybeId);"));
        assertTrue(trace.contains("recordId(fallbackId);"));
        assertTrue(trace.contains("Replacement: `fallbackId`"));
        assertTrue(trace.contains("Inference solved: `true`"));
        assertTrue(trace.contains("Final typecheck verified: `true`"));
        assertTrue(
                trace.contains(
                        "Accepted edit: `replace nullable expression with in-scope non-null String local`"));
    }

    @Test
    public void mainWritesCaseStudyTrace() throws Exception {
        File output = new File("build/inference-repair-case-study-test/trace.md");

        InferenceRepairCaseStudyMain.main(
                new String[] {
                    "--source", "testdata/repair/InferenceUnsatMethodCall.java",
                    "--out", output.getPath()
                });

        String trace = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(trace.contains("Source: `testdata/repair/InferenceUnsatMethodCall.java`"));
        assertTrue(trace.contains("Conclusion"));
    }
}
