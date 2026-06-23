package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.solver.MaxSat2TypeSolver;

import org.junit.Test;

public class InferenceFirstPipelineTest {
    @Test
    public void capturesRealInferenceSnapshotBeforeRepair() {
        InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--checker",
                            "nninf.NninfChecker",
                            "--solver",
                            MaxSat2TypeSolver.class.getCanonicalName(),
                            "--jaifFile",
                            "build/inference-first-pipeline/assignment.jaif",
                            "--hacks=true",
                            "--",
                            "-Anomsgtext",
                            "-d",
                            "tests/build/outputdir",
                            "testdata/repair/AssignmentRepair.java"
                        },
                        false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        inferenceMain.run();

        InferenceRunSnapshot snapshot = inferenceMain.getRunSnapshot();
        assertNotNull(snapshot);
        assertFalse(snapshot.getSlots().isEmpty());
        assertFalse(snapshot.getConstraints().isEmpty());
        assertNotNull(snapshot.getSolverResult());
        assertTrue(snapshot.hasSolution());
    }
}
