package checkers.inference.repair;

/** Experiment-level settings for ablation runs. */
public final class InferenceRepairExperimentConfig {
    private final InferenceRepairExperimentMode mode;
    private final InferenceRepairEditProviderMode editProviderMode;

    public InferenceRepairExperimentConfig(InferenceRepairExperimentMode mode) {
        this(mode, InferenceRepairEditProviderMode.DETERMINISTIC_ONLY);
    }

    public InferenceRepairExperimentConfig(
            InferenceRepairExperimentMode mode,
            InferenceRepairEditProviderMode editProviderMode) {
        this.mode = mode;
        this.editProviderMode = editProviderMode;
    }

    public static InferenceRepairExperimentConfig unsatCoreGuided() {
        return new InferenceRepairExperimentConfig(InferenceRepairExperimentMode.UNSAT_CORE_GUIDED);
    }

    public static InferenceRepairExperimentConfig unsatCoreOnly() {
        return new InferenceRepairExperimentConfig(InferenceRepairExperimentMode.UNSAT_CORE_ONLY);
    }

    public static InferenceRepairExperimentConfig noRepair() {
        return new InferenceRepairExperimentConfig(InferenceRepairExperimentMode.NO_REPAIR);
    }

    public InferenceRepairExperimentConfig withEditProviderMode(
            InferenceRepairEditProviderMode editProviderMode) {
        return new InferenceRepairExperimentConfig(mode, editProviderMode);
    }

    public InferenceRepairExperimentMode getMode() {
        return mode;
    }

    public InferenceRepairEditProviderMode getEditProviderMode() {
        return editProviderMode;
    }

    public boolean shouldRunRepair() {
        return mode != InferenceRepairExperimentMode.NO_REPAIR;
    }

    public boolean shouldExpandRepairContexts() {
        return mode == InferenceRepairExperimentMode.UNSAT_CORE_GUIDED;
    }
}
