package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;
import checkers.inference.model.Constraint;
import checkers.inference.solver.MaxSat2TypeSolver;

import java.io.File;
import java.util.List;

import org.junit.Test;

public class InferenceUnsatRepairPipelineTest {
    private static final File UNSAT_FIXTURE =
            new File("testdata/repair/InferenceUnsatAssignment.java");

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
                        .planFromInferenceContexts(report.getUnsatConstraintContexts());

        for (InferenceRepairCandidate candidate : candidates) {
            System.out.println("candidate: " + candidate.summarize());
        }

        assertFalse(report.getUnsatConstraintContexts().isEmpty());
        assertFalse(candidates.isEmpty());
        assertTrue(
                candidates.get(0).getTargetSlot().getLocation().contains("InferenceUnsatAssignment"));

        InferenceRepairValidationResult validationResult =
                new SimpleNninfInferenceRepairValidator(
                                UNSAT_FIXTURE, new File("build/inference-repair-validation"))
                        .validate(candidates.get(0));

        System.out.println("applied: " + validationResult.getAppliedEdit());
        System.out.println(
                "rerun inference solved: " + validationResult.getSnapshot().hasSolution());

        assertTrue(validationResult.solvesInference());
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
