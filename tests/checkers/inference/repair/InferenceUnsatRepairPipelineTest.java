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

import java.util.List;

import org.junit.Test;

public class InferenceUnsatRepairPipelineTest {
    @Test
    public void plansRepairCandidateFromRealUnsatInferenceRun() {
        InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--checker",
                            "nninf.NninfChecker",
                            "--solver",
                            MaxSat2TypeSolver.class.getCanonicalName(),
                            "--jaifFile",
                            "build/inference-unsat-pipeline/unsat.jaif",
                            "--hacks=true",
                            "--",
                            "-Anomsgtext",
                            "-d",
                            "tests/build/outputdir",
                            "testdata/repair/InferenceUnsatAssignment.java"
                        },
                        false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        InferenceRunSnapshot snapshot;
        try {
            inferenceMain.run();
            snapshot = inferenceMain.getRunSnapshot();
        } catch (InferenceUnsatisfiableException e) {
            snapshot = e.getSnapshot();
        }
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
    }
}
