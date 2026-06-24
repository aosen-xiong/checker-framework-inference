package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;
import checkers.inference.model.Constraint;
import checkers.inference.solver.MaxSat2TypeSolver;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class InferenceUnsatRepairPipelineTest {
    private static final File UNSAT_FIXTURE =
            new File("testdata/repair/InferenceUnsatAssignment.java");
    private static final File FIELD_ASSIGNMENT_UNSAT_FIXTURE =
            new File("testdata/repair/InferenceUnsatFieldAssignment.java");
    private static final File METHOD_CALL_UNSAT_FIXTURE =
            new File("testdata/repair/InferenceUnsatMethodCall.java");

    @Test
    public void plansRepairCandidateFromRealUnsatInferenceRun() {
        InferenceRunSnapshot snapshot = runInference(UNSAT_FIXTURE, "unsat");
        assertNotNull(snapshot);

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
        System.out.println("=== real inference-unsat repair pipeline ===");
        System.out.println(report.summarize());
        for (Constraint constraint : snapshot.getConstraints()) {
            System.out.println(
                    "constraint: "
                            + InferenceConstraintContextFormatter.format(constraint).summarize());
        }
        assertFalse(snapshot.hasSolution());

        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(report.getRepairConstraintContexts());

        for (InferenceRepairCandidate candidate : candidates) {
            System.out.println("candidate: " + candidate.summarize());
        }

        assertFalse(report.getUnsatConstraintContexts().isEmpty());
        assertFalse(report.getRepairConstraintContexts().isEmpty());
        assertFalse(candidates.isEmpty());
        assertTrue(
                candidates.get(0).getTargetSlot().getLocation().contains("InferenceUnsatAssignment"));

        InferenceRepairSearchResult searchResult =
                new SimpleNninfInferenceRepairValidator(
                                UNSAT_FIXTURE, new File("build/inference-repair-validation"))
                        .validateAll(withUnsupportedCandidateFirst(candidates));
        assertTrue(searchResult.getValidationResults().get(0).hasValidationError());

        InferenceRepairValidationResult validationResult = searchResult.getPassingResult();
        assertNotNull(validationResult);

        for (InferenceRepairAttempt attempt : validationResult.getAttempts()) {
            System.out.println(
                    "attempt: "
                            + attempt.getRepairKind()
                            + " target="
                            + attempt.getTarget().summarize()
                            + " -> solved="
                            + attempt.solvesInference()
                            + ", edit="
                            + attempt.getAppliedEdit());
        }

        assertEquals(2, validationResult.getAttempts().size());
        assertEquals(
                InferenceRepairKind.INSERT_NULL_GUARD,
                validationResult.getAttempts().get(0).getRepairKind());
        assertEquals("VARIABLE", validationResult.getAttempts().get(0).getTarget().getTreeKind());
        assertTrue(
                validationResult
                        .getAttempts()
                        .get(0)
                        .getTarget()
                        .getOriginalText()
                        .contains("@NonNull String id = maybeId"));
        assertFalse(validationResult.getAttempts().get(0).solvesInference());
        assertTrue(
                repairedSourceText(validationResult.getAttempts().get(0))
                        .contains(
                                "if (maybeId == null) { return; }\n"
                                        + "        @NonNull String id = maybeId;"));
        assertEquals(
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                validationResult.getPassingAttempt().getRepairKind());
        assertTrue(
                repairedSourceText(validationResult.getPassingAttempt())
                        .contains("@NonNull String id = \"\";"));
        assertPostVerified(validationResult.getPassingAttempt());
        assertTrue(validationResult.solvesInference());
        assertTrue(searchResult.solvesInference());
        assertTrue(searchResult.isFullyVerified());
    }

    @Test
    public void repairsRealUnsatFieldAssignmentExpression() {
        InferenceRunSnapshot snapshot =
                runInference(FIELD_ASSIGNMENT_UNSAT_FIXTURE, "field-assignment");
        assertNotNull(snapshot);
        assertFalse(snapshot.hasSolution());

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(report.getRepairConstraintContexts());
        assertFalse(candidates.isEmpty());

        InferenceRepairSearchResult searchResult =
                new SimpleNninfInferenceRepairValidator(
                                FIELD_ASSIGNMENT_UNSAT_FIXTURE,
                                new File("build/inference-field-assignment-repair-validation"))
                        .validateAll(candidates);

        InferenceRepairValidationResult validationResult = searchResult.getPassingResult();
        assertNotNull(validationResult);
        InferenceRepairAttempt passingAttempt = validationResult.getPassingAttempt();
        assertEquals(
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                passingAttempt.getRepairKind());
        assertEquals("IDENTIFIER", passingAttempt.getTarget().getTreeKind());
        assertEquals("maybeId", passingAttempt.getTarget().getOriginalText());
        assertTrue(repairedSourceText(passingAttempt).contains("id = \"\";"));
        assertPostVerified(passingAttempt);
        assertTrue(searchResult.solvesInference());
        assertTrue(searchResult.isFullyVerified());
    }

    @Test
    public void repairsRealUnsatMethodCallArgumentExpression() {
        InferenceRunSnapshot snapshot = runInference(METHOD_CALL_UNSAT_FIXTURE, "method-call");
        assertNotNull(snapshot);
        assertFalse(snapshot.hasSolution());

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(report.getRepairConstraintContexts());
        assertFalse(candidates.isEmpty());

        InferenceRepairSearchResult searchResult =
                new InferenceRepairValidator(
                                InferenceRepairConfiguration.nninfDefault(),
                                METHOD_CALL_UNSAT_FIXTURE,
                                new File("build/inference-method-call-repair-validation"))
                        .validateAll(candidates);

        InferenceRepairValidationResult validationResult = searchResult.getPassingResult();
        assertNotNull(validationResult);
        InferenceRepairAttempt passingAttempt = validationResult.getPassingAttempt();
        assertEquals(
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                passingAttempt.getRepairKind());
        assertEquals("IDENTIFIER", passingAttempt.getTarget().getTreeKind());
        assertEquals("maybeId", passingAttempt.getTarget().getOriginalText());
        assertEquals(
                "replace nullable expression with in-scope non-null String local",
                passingAttempt.getAppliedEdit());
        assertTrue(repairedSourceText(passingAttempt).contains("recordId(fallbackId);"));
        assertPostVerified(passingAttempt);
        assertTrue(searchResult.solvesInference());
        assertTrue(searchResult.isFullyVerified());
    }

    @Test
    public void validatesInjectedRepairEditProvider() {
        InferenceRunSnapshot snapshot =
                runInference(METHOD_CALL_UNSAT_FIXTURE, "method-call-injected");
        assertNotNull(snapshot);
        assertFalse(snapshot.hasSolution());

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(report.getRepairConstraintContexts());
        assertFalse(candidates.isEmpty());

        InferenceRepairSearchResult searchResult =
                new InferenceRepairValidator(
                                InferenceRepairConfiguration.nninfDefault(),
                                METHOD_CALL_UNSAT_FIXTURE,
                                new File("build/inference-method-call-injected-repair-validation"),
                                new FixedFallbackEditProvider())
                        .validateAll(candidates);

        InferenceRepairValidationResult validationResult = searchResult.getPassingResult();
        assertNotNull(validationResult);
        InferenceRepairAttempt passingAttempt = validationResult.getPassingAttempt();
        assertTrue(repairedSourceText(passingAttempt).contains("recordId(fallbackId);"));
        assertPostVerified(passingAttempt);
        assertTrue(searchResult.isFullyVerified());
    }

    private static void assertPostVerified(InferenceRepairAttempt attempt) {
        assertTrue(attempt.isFullyVerified());
        InferenceRepairPostVerificationResult postVerification =
                attempt.getPostVerificationResult();
        assertNotNull(postVerification);
        assertNotNull(postVerification.getAnnotatedSourceFile());
        assertFalse(postVerification.didInsertionFail());
        assertFalse(postVerification.didTypecheckFail());
    }

    private static List<InferenceRepairCandidate> withUnsupportedCandidateFirst(
            List<InferenceRepairCandidate> candidates) {
        List<InferenceRepairCandidate> candidatesWithUnsupported = new ArrayList<>();
        candidatesWithUnsupported.add(
                new InferenceRepairCandidate(
                        candidates.get(0).getConstraintContext(),
                        new InferenceSlotContext(
                                99,
                                "VARIABLE",
                                true,
                                "CLASS_DECL",
                                "ClassDeclLocation( InferenceUnsatAssignment )",
                                "slot#99"),
                        InferenceRepairKind.WEAKEN_ANNOTATION,
                        "@Nullable",
                        "unsupported class-declaration repair candidate"));
        candidatesWithUnsupported.addAll(candidates);
        return candidatesWithUnsupported;
    }

    private static String repairedSourceText(InferenceRepairAttempt attempt) {
        List<String> lines = InferenceTestUtilities.getLines(attempt.getRepairedSourceFile());
        return String.join("\n", lines) + "\n";
    }

    private static final class FixedFallbackEditProvider implements InferenceRepairEditProvider {
        @Override
        public List<InferenceRepairEdit> generate(
                InferenceRepairCandidate candidate,
                InferenceRepairTarget target,
                String originalSource) {
            return Collections.singletonList(
                    new InferenceRepairEdit(
                            InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                            "fallbackId",
                            "replace nullable expression with injected fallback",
                            "replace_with_injected_fallback"));
        }
    }

    private static InferenceRunSnapshot runInference(File sourceFile, String jaifBaseName) {
        InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--checker",
                            "nninf.NninfChecker",
                            "--solver",
                            MaxSat2TypeSolver.class.getCanonicalName(),
                            "--jaifFile",
                            "build/inference-unsat-pipeline/" + jaifBaseName + ".jaif",
                            "--hacks=true",
                            "--",
                            "-Anomsgtext",
                            "-d",
                            "tests/build/outputdir",
                            sourceFile.getPath()
                        },
                        false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        try {
            inferenceMain.run();
            return inferenceMain.getRunSnapshot();
        } catch (InferenceUnsatisfiableException e) {
            return e.getSnapshot();
        }
    }
}
