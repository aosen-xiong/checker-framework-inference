package checkers.inference.repair;

import checkers.inference.solver.MaxSat2TypeSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Checker-specific settings for an inference-guided repair validation run. */
public final class InferenceRepairConfiguration {
    private final Class<?> checker;
    private final String solver;
    private final List<String> inferenceJavacOptions;
    private final List<String> typecheckJavacOptions;
    private final boolean useHacks;

    public InferenceRepairConfiguration(
            Class<?> checker,
            String solver,
            List<String> inferenceJavacOptions,
            List<String> typecheckJavacOptions,
            boolean useHacks) {
        this.checker = checker;
        this.solver = solver;
        this.inferenceJavacOptions =
                Collections.unmodifiableList(new ArrayList<>(inferenceJavacOptions));
        this.typecheckJavacOptions =
                Collections.unmodifiableList(new ArrayList<>(typecheckJavacOptions));
        this.useHacks = useHacks;
    }

    public static InferenceRepairConfiguration nninfDefault() {
        List<String> javacOptions = Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");
        return new InferenceRepairConfiguration(
                nninf.NninfChecker.class,
                MaxSat2TypeSolver.class.getCanonicalName(),
                javacOptions,
                javacOptions,
                true);
    }

    public Class<?> getChecker() {
        return checker;
    }

    public String getSolver() {
        return solver;
    }

    public List<String> getInferenceJavacOptions() {
        return inferenceJavacOptions;
    }

    public List<String> getTypecheckJavacOptions() {
        return typecheckJavacOptions;
    }

    public boolean shouldUseHacks() {
        return useHacks;
    }
}
