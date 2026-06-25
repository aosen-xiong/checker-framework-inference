package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import checkers.inference.InferenceResult;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.model.Constraint;
import checkers.inference.solver.MaxSat2TypeSolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import org.junit.Test;

public class InferenceRepairValidatorTest {
    private static final File METHOD_CALL_UNSAT_FIXTURE =
            new File("testdata/repair/InferenceUnsatMethodCall.java");

    @Test
    public void stopsAtConfiguredEditBudgetWithoutRunningCompiler() {
        FakeRunner runner = new FakeRunner(false);
        InferenceRepairValidator validator =
                new InferenceRepairValidator(
                        configurationWithBudgets(Integer.MAX_VALUE, 1),
                        METHOD_CALL_UNSAT_FIXTURE,
                        new File("build/inference-repair-validator-test/edit-budget"),
                        new TwoEditProvider(),
                        runner);

        InferenceRepairValidationResult result = validator.validate(methodCallCandidate());

        assertEquals(1, result.getAttempts().size());
        assertEquals(1, runner.getRuns().size());
        assertEquals("maybeId", result.getAttempts().get(0).getReplacementSource());
    }

    @Test
    public void validatesAllStopsAtConfiguredCandidateBudgetWithoutRunningCompiler() {
        FakeRunner runner = new FakeRunner(false);
        InferenceRepairValidator validator =
                new InferenceRepairValidator(
                        configurationWithBudgets(1, Integer.MAX_VALUE),
                        METHOD_CALL_UNSAT_FIXTURE,
                        new File("build/inference-repair-validator-test/candidate-budget"),
                        new TwoEditProvider(),
                        runner);

        InferenceRepairSearchResult result =
                validator.validateAll(Arrays.asList(methodCallCandidate(), methodCallCandidate()));

        assertEquals(1, result.getValidationResults().size());
        assertEquals(2, runner.getRuns().size());
    }

    private static InferenceRepairConfiguration configurationWithBudgets(
            int maxCandidatesToValidate, int maxEditsPerCandidate) {
        List<String> javacOptions = Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");
        return new InferenceRepairConfiguration(
                nninf.NninfChecker.class,
                MaxSat2TypeSolver.class.getCanonicalName(),
                javacOptions,
                javacOptions,
                true,
                maxCandidatesToValidate,
                maxEditsPerCandidate);
    }

    private static InferenceRepairCandidate methodCallCandidate() {
        return new InferenceRepairCandidate(
                null,
                new InferenceSlotContext(
                        7,
                        "REFINEMENT_VARIABLE",
                        false,
                        "AST_PATH",
                        "AstPathLocation( InferenceUnsatMethodCall.setId(Ljava/lang/String;)V.null:"
                                + "InferenceUnsatMethodCall:setId(Ljava/lang/String;)V::"
                                + "Method.body, Block.statement 1,"
                                + " ExpressionStatement.expression,"
                                + " MethodInvocation.argument 0 )",
                        "slot#7"),
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                "@Nullable",
                "repair source expression causing @Nullable conflict");
    }

    private static final class TwoEditProvider implements InferenceRepairEditProvider {
        @Override
        public List<InferenceRepairEdit> generate(
                InferenceRepairCandidate candidate,
                InferenceRepairTarget target,
                String originalSource) {
            List<InferenceRepairEdit> edits = new ArrayList<>();
            edits.add(
                    new InferenceRepairEdit(
                            InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                            "maybeId",
                            "keep nullable expression unchanged",
                            "keep_nullable_expression"));
            edits.add(
                    new InferenceRepairEdit(
                            InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                            "fallbackId",
                            "replace nullable expression with valid fallback",
                            "replace_with_valid_fallback"));
            return edits;
        }
    }

    private static final class FakeRunner implements InferenceRepairRunner {
        private final boolean hasSolution;
        private final List<File> runs = new ArrayList<>();

        private FakeRunner(boolean hasSolution) {
            this.hasSolution = hasSolution;
        }

        @Override
        public InferenceRepairRunResult run(
                File repairedSourceFile, File jaifFile, File annotatedSourceDirectory) {
            runs.add(repairedSourceFile);
            return new InferenceRepairRunResult(snapshot(hasSolution), null);
        }

        private List<File> getRuns() {
            return runs;
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
