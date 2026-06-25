package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.InferenceResult;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.model.Constraint;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import org.junit.Test;

public class SourceRepairPlanValidatorTest {
    @Test
    public void validatesMaterializedTopPlanWithInjectedRunner() {
        FakeRunner runner = new FakeRunner(false);
        SourceRepairPlan plan =
                new SourceRepairPlan(
                        1,
                        1,
                        Collections.singletonList(
                                new SourceRepairPlanStep(
                                        "slot#7:AST_PATH:AstPathLocation( "
                                                + "InferenceUnsatMethodCall.recordId"
                                                + "(Ljava/lang/String;)V.null:"
                                                + "InferenceUnsatMethodCall:recordId"
                                                + "(Ljava/lang/String;)V::Method.parameter 0, "
                                                + "Variable.type )",
                                        "InsertQualifier",
                                        "@nninf.qual.Nullable",
                                        1)));

        SourceRepairPlanValidationResult result =
                new SourceRepairPlanValidator(new SourceRepairPlanMaterializer(), runner)
                        .validateTopPlan(
                                new File("testdata/repair/InferenceUnsatMethodCall.java"),
                                new File("build/source-repair-plan-validator-test"),
                                Collections.singletonList(plan));

        assertTrue(result.isMaterialized());
        assertFalse(result.isInferenceSolved());
        assertFalse(result.isVerified());
        assertEquals(1, runner.runCount);
        assertTrue(runner.lastSourceFile.getPath().endsWith("InferenceUnsatMethodCall.java"));
        assertEquals(4, result.getStages().size());
        assertEquals("annotation-materialization", result.getStages().get(0).getStage());
        assertEquals("inference-rerun", result.getStages().get(1).getStage());
        assertEquals("post-inference-typecheck", result.getStages().get(2).getStage());
        assertEquals("diagnostic-source-repair", result.getStages().get(3).getStage());
        assertFalse(result.getStages().get(1).isSucceeded());
        assertFalse(result.getStages().get(3).isVerified());
    }

    @Test
    public void reportsNoPlanWithoutInvokingRunner() {
        FakeRunner runner = new FakeRunner(true);

        SourceRepairPlanValidationResult result =
                new SourceRepairPlanValidator(new SourceRepairPlanMaterializer(), runner)
                        .validateTopPlan(
                                new File("testdata/repair/InferenceUnsatMethodCall.java"),
                                new File("build/source-repair-plan-validator-test"),
                                Collections.<SourceRepairPlan>emptyList());

        assertFalse(result.isMaterialized());
        assertEquals("NO_PLAN", result.getError());
        assertEquals(1, result.getStages().size());
        assertEquals("plan-selection", result.getStages().get(0).getStage());
        assertEquals(0, runner.runCount);
    }

    private static final class FakeRunner implements InferenceRepairRunner {
        private final boolean hasSolution;
        private int runCount;
        private File lastSourceFile;

        private FakeRunner(boolean hasSolution) {
            this.hasSolution = hasSolution;
        }

        @Override
        public InferenceRepairRunResult run(
                File repairedSourceFile, File jaifFile, File annotatedSourceDirectory) {
            runCount++;
            lastSourceFile = repairedSourceFile;
            return new InferenceRepairRunResult(snapshot(hasSolution), null);
        }
    }

    private static InferenceRunSnapshot snapshot(boolean hasSolution) {
        return new InferenceRunSnapshot(
                Collections.emptyList(),
                Collections.emptyList(),
                new FakeInferenceResult(hasSolution));
    }

    private static final class FakeInferenceResult implements InferenceResult {
        private final boolean hasSolution;

        private FakeInferenceResult(boolean hasSolution) {
            this.hasSolution = hasSolution;
        }

        @Override
        public boolean hasSolution() {
            return hasSolution;
        }

        @Override
        public Map<Integer, AnnotationMirror> getSolutions() {
            return Collections.emptyMap();
        }

        @Override
        public boolean containsSolutionForVariable(int varId) {
            return false;
        }

        @Override
        public AnnotationMirror getSolutionForVariable(int varId) {
            return null;
        }

        @Override
        public Collection<Constraint> getUnsatisfiableConstraints() {
            return Collections.emptyList();
        }
    }
}
