package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of materializing and validating one source repair plan. */
public final class SourceRepairPlanValidationResult {
    private final SourceRepairPlan plan;
    private final boolean materialized;
    private final File repairedSourceFile;
    private final boolean inferenceSolved;
    private final boolean verified;
    private final boolean followUpRepairAttempted;
    private final boolean followUpRepairVerified;
    private final File followUpRepairedSourceFile;
    private final String followUpAppliedEdit;
    private final String followUpReplacementSource;
    private final List<RepairStageTrace> stages;
    private final String error;

    private SourceRepairPlanValidationResult(
            SourceRepairPlan plan,
            boolean materialized,
            File repairedSourceFile,
            boolean inferenceSolved,
            boolean verified,
            boolean followUpRepairAttempted,
            boolean followUpRepairVerified,
            File followUpRepairedSourceFile,
            String followUpAppliedEdit,
            String followUpReplacementSource,
            List<RepairStageTrace> stages,
            String error) {
        this.plan = plan;
        this.materialized = materialized;
        this.repairedSourceFile = repairedSourceFile;
        this.inferenceSolved = inferenceSolved;
        this.verified = verified;
        this.followUpRepairAttempted = followUpRepairAttempted;
        this.followUpRepairVerified = followUpRepairVerified;
        this.followUpRepairedSourceFile = followUpRepairedSourceFile;
        this.followUpAppliedEdit = followUpAppliedEdit;
        this.followUpReplacementSource = followUpReplacementSource;
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
        this.error = error;
    }

    public static SourceRepairPlanValidationResult noPlan() {
        return new SourceRepairPlanValidationResult(
                null,
                false,
                null,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                Collections.singletonList(
                        new RepairStageTrace(
                                "plan-selection",
                                "select top source repair plan",
                                true,
                                false,
                                false,
                                null,
                                null,
                                null,
                                "NO_PLAN")),
                "NO_PLAN");
    }

    public static SourceRepairPlanValidationResult materializationFailed(
            SourceRepairPlan plan, String error) {
        return new SourceRepairPlanValidationResult(
                plan,
                false,
                null,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                Collections.singletonList(
                        new RepairStageTrace(
                                "annotation-materialization",
                                "materialize selected annotation repair plan",
                                true,
                                false,
                                false,
                                null,
                                null,
                                null,
                                error)),
                error);
    }

    public static SourceRepairPlanValidationResult validated(
            SourceRepairPlan plan,
            File repairedSourceFile,
            boolean inferenceSolved,
            boolean verified,
            String error) {
        return new SourceRepairPlanValidationResult(
                plan,
                true,
                repairedSourceFile,
                inferenceSolved,
                verified,
                false,
                false,
                null,
                null,
                null,
                validationStages(plan, repairedSourceFile, inferenceSolved, verified, error),
                error);
    }

    public static SourceRepairPlanValidationResult sourceRepairValidated(
            SourceRepairPlan plan,
            File repairedSourceFile,
            boolean inferenceSolved,
            boolean verified,
            String appliedEdit,
            String replacementSource,
            String error) {
        return new SourceRepairPlanValidationResult(
                plan,
                false,
                null,
                inferenceSolved,
                verified,
                false,
                false,
                null,
                null,
                null,
                sourceRepairStages(
                        repairedSourceFile,
                        inferenceSolved,
                        verified,
                        appliedEdit,
                        replacementSource,
                        error),
                error);
    }

    public SourceRepairPlanValidationResult withFollowUpRepair(
            boolean followUpRepairVerified,
            File followUpRepairedSourceFile,
            String followUpAppliedEdit,
            String followUpReplacementSource) {
        return withFollowUpRepair(
                true,
                followUpRepairVerified,
                followUpRepairedSourceFile,
                followUpAppliedEdit,
                followUpReplacementSource);
    }

    public SourceRepairPlanValidationResult withSkippedFollowUpRepair(String error) {
        return withFollowUpRepair(false, false, null, null, null, error);
    }

    private SourceRepairPlanValidationResult withFollowUpRepair(
            boolean followUpRepairAttempted,
            boolean followUpRepairVerified,
            File followUpRepairedSourceFile,
            String followUpAppliedEdit,
            String followUpReplacementSource) {
        return withFollowUpRepair(
                followUpRepairAttempted,
                followUpRepairVerified,
                followUpRepairedSourceFile,
                followUpAppliedEdit,
                followUpReplacementSource,
                followUpRepairVerified ? null : "FOLLOW_UP_REPAIR_NOT_VERIFIED");
    }

    private SourceRepairPlanValidationResult withFollowUpRepair(
            boolean followUpRepairAttempted,
            boolean followUpRepairVerified,
            File followUpRepairedSourceFile,
            String followUpAppliedEdit,
            String followUpReplacementSource,
            String stageError) {
        return new SourceRepairPlanValidationResult(
                plan,
                materialized,
                repairedSourceFile,
                inferenceSolved,
                verified || followUpRepairVerified,
                followUpRepairAttempted,
                followUpRepairVerified,
                followUpRepairedSourceFile,
                followUpAppliedEdit,
                followUpReplacementSource,
                withFollowUpStage(
                        followUpRepairAttempted,
                        followUpRepairVerified,
                        followUpRepairedSourceFile,
                        followUpAppliedEdit,
                        followUpReplacementSource,
                        stageError),
                error);
    }

    private static List<RepairStageTrace> validationStages(
            SourceRepairPlan plan,
            File repairedSourceFile,
            boolean inferenceSolved,
            boolean verified,
            String error) {
        List<RepairStageTrace> stages = new ArrayList<>();
        stages.add(
                new RepairStageTrace(
                        "annotation-materialization",
                        "materialize selected annotation repair plan",
                        true,
                        repairedSourceFile != null,
                        false,
                        repairedSourceFile,
                        plan == null ? null : "source repair plan rank " + plan.getRank(),
                        null,
                        error));
        stages.add(
                new RepairStageTrace(
                        "inference-rerun",
                        "rerun inference on materialized source",
                        true,
                        inferenceSolved,
                        false,
                        repairedSourceFile,
                        null,
                        null,
                        inferenceSolved || error != null ? error : "INFERENCE_UNSOLVED"));
        stages.add(
                new RepairStageTrace(
                        "post-inference-typecheck",
                        "insert inferred annotations and typecheck",
                        inferenceSolved,
                        verified,
                        verified,
                        null,
                        null,
                        null,
                        verified || !inferenceSolved ? null : "RESIDUAL_DIAGNOSTICS"));
        return stages;
    }

    private static List<RepairStageTrace> sourceRepairStages(
            File repairedSourceFile,
            boolean inferenceSolved,
            boolean verified,
            String appliedEdit,
            String replacementSource,
            String error) {
        List<RepairStageTrace> stages = new ArrayList<>();
        stages.add(
                new RepairStageTrace(
                        "inference-source-repair",
                        "repair expression source site from inference constraint",
                        true,
                        inferenceSolved,
                        verified,
                        repairedSourceFile,
                        appliedEdit,
                        replacementSource,
                        inferenceSolved ? null : error));
        return stages;
    }

    private List<RepairStageTrace> withFollowUpStage(
            boolean followUpRepairAttempted,
            boolean followUpRepairVerified,
            File followUpRepairedSourceFile,
            String followUpAppliedEdit,
            String followUpReplacementSource,
            String stageError) {
        List<RepairStageTrace> updatedStages = new ArrayList<>(stages);
        updatedStages.add(
                new RepairStageTrace(
                        "diagnostic-source-repair",
                        "repair residual checker diagnostic",
                        followUpRepairAttempted,
                        followUpRepairVerified,
                        followUpRepairVerified,
                        followUpRepairedSourceFile,
                        followUpAppliedEdit,
                        followUpReplacementSource,
                        stageError));
        return updatedStages;
    }

    public SourceRepairPlan getPlan() {
        return plan;
    }

    public boolean isMaterialized() {
        return materialized;
    }

    public File getRepairedSourceFile() {
        return repairedSourceFile;
    }

    public boolean isInferenceSolved() {
        return inferenceSolved;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isFollowUpRepairAttempted() {
        return followUpRepairAttempted;
    }

    public boolean isFollowUpRepairVerified() {
        return followUpRepairVerified;
    }

    public File getFollowUpRepairedSourceFile() {
        return followUpRepairedSourceFile;
    }

    public String getFollowUpAppliedEdit() {
        return followUpAppliedEdit;
    }

    public String getFollowUpReplacementSource() {
        return followUpReplacementSource;
    }

    public List<RepairStageTrace> getStages() {
        return stages;
    }

    public String getError() {
        return error;
    }
}
