package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import checkers.inference.solver.MaxSat2TypeSolver;

import org.junit.Test;

public class InferenceRepairConfigurationTest {
    @Test
    public void nninfDefaultCarriesCheckerSolverAndOptions() {
        InferenceRepairConfiguration configuration = InferenceRepairConfiguration.nninfDefault();

        assertEquals(nninf.NninfChecker.class, configuration.getChecker());
        assertEquals(MaxSat2TypeSolver.class.getCanonicalName(), configuration.getSolver());
        assertTrue(configuration.getInferenceJavacOptions().contains("-Anomsgtext"));
        assertTrue(configuration.getTypecheckJavacOptions().contains("-Anomsgtext"));
        assertTrue(configuration.shouldUseHacks());
    }

    @Test
    public void optionListsAreImmutableSnapshots() {
        InferenceRepairConfiguration configuration = InferenceRepairConfiguration.nninfDefault();

        try {
            configuration.getInferenceJavacOptions().add("-AnewOption");
        } catch (UnsupportedOperationException expected) {
            return;
        }

        fail("Expected immutable inference javac options.");
    }
}
