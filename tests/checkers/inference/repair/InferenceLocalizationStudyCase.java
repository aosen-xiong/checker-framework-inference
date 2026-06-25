package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One source file's localization-study record. */
public final class InferenceLocalizationStudyCase {
    private final File sourceFile;
    private final String runError;
    private final boolean solverHadSolution;
    private final int constraintCount;
    private final int unsatConstraintCount;
    private final String mcsOracleKind;
    private final int mcsOriginalUniverseSize;
    private final int mcsEnumeratedUniverseSize;
    private final boolean mcsUniverseTruncated;
    private final int mcsMaxRemovalSize;
    private final boolean mcsSearchBounded;
    private final List<String> unsatCoreLocations;
    private final List<SourceRepairUnit> sourceRepairUnits;
    private final List<SourceRepairPlan> sourceRepairPlans;
    private final SourceRepairPlanValidationResult sourceRepairPlanValidationResult;
    private final List<InferenceLocalizationStudyCandidate> weightedMcsCandidates;

    public InferenceLocalizationStudyCase(
            File sourceFile,
            String runError,
            boolean solverHadSolution,
            int constraintCount,
            int unsatConstraintCount,
            String mcsOracleKind,
            int mcsOriginalUniverseSize,
            int mcsEnumeratedUniverseSize,
            boolean mcsUniverseTruncated,
            int mcsMaxRemovalSize,
            boolean mcsSearchBounded,
            List<String> unsatCoreLocations,
            List<SourceRepairUnit> sourceRepairUnits,
            List<SourceRepairPlan> sourceRepairPlans,
            List<InferenceLocalizationStudyCandidate> weightedMcsCandidates) {
        this(
                sourceFile,
                runError,
                solverHadSolution,
                constraintCount,
                unsatConstraintCount,
                mcsOracleKind,
                mcsOriginalUniverseSize,
                mcsEnumeratedUniverseSize,
                mcsUniverseTruncated,
                mcsMaxRemovalSize,
                mcsSearchBounded,
                unsatCoreLocations,
                sourceRepairUnits,
                sourceRepairPlans,
                null,
                weightedMcsCandidates);
    }

    public InferenceLocalizationStudyCase(
            File sourceFile,
            String runError,
            boolean solverHadSolution,
            int constraintCount,
            int unsatConstraintCount,
            String mcsOracleKind,
            int mcsOriginalUniverseSize,
            int mcsEnumeratedUniverseSize,
            boolean mcsUniverseTruncated,
            int mcsMaxRemovalSize,
            boolean mcsSearchBounded,
            List<String> unsatCoreLocations,
            List<SourceRepairUnit> sourceRepairUnits,
            List<SourceRepairPlan> sourceRepairPlans,
            SourceRepairPlanValidationResult sourceRepairPlanValidationResult,
            List<InferenceLocalizationStudyCandidate> weightedMcsCandidates) {
        this.sourceFile = sourceFile;
        this.runError = runError;
        this.solverHadSolution = solverHadSolution;
        this.constraintCount = constraintCount;
        this.unsatConstraintCount = unsatConstraintCount;
        this.mcsOracleKind = mcsOracleKind;
        this.mcsOriginalUniverseSize = mcsOriginalUniverseSize;
        this.mcsEnumeratedUniverseSize = mcsEnumeratedUniverseSize;
        this.mcsUniverseTruncated = mcsUniverseTruncated;
        this.mcsMaxRemovalSize = mcsMaxRemovalSize;
        this.mcsSearchBounded = mcsSearchBounded;
        this.unsatCoreLocations =
                Collections.unmodifiableList(new ArrayList<>(unsatCoreLocations));
        this.sourceRepairUnits =
                Collections.unmodifiableList(new ArrayList<>(sourceRepairUnits));
        this.sourceRepairPlans =
                Collections.unmodifiableList(new ArrayList<>(sourceRepairPlans));
        this.sourceRepairPlanValidationResult = sourceRepairPlanValidationResult;
        this.weightedMcsCandidates =
                Collections.unmodifiableList(new ArrayList<>(weightedMcsCandidates));
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getRunError() {
        return runError;
    }

    public boolean solverHadSolution() {
        return solverHadSolution;
    }

    public int getConstraintCount() {
        return constraintCount;
    }

    public int getUnsatConstraintCount() {
        return unsatConstraintCount;
    }

    public String getMcsOracleKind() {
        return mcsOracleKind;
    }

    public int getMcsOriginalUniverseSize() {
        return mcsOriginalUniverseSize;
    }

    public int getMcsEnumeratedUniverseSize() {
        return mcsEnumeratedUniverseSize;
    }

    public boolean isMcsUniverseTruncated() {
        return mcsUniverseTruncated;
    }

    public int getMcsMaxRemovalSize() {
        return mcsMaxRemovalSize;
    }

    public boolean isMcsSearchBounded() {
        return mcsSearchBounded;
    }

    public List<String> getUnsatCoreLocations() {
        return unsatCoreLocations;
    }

    public List<SourceRepairUnit> getSourceRepairUnits() {
        return sourceRepairUnits;
    }

    public int getSourceRepairUnitCount() {
        return sourceRepairUnits.size();
    }

    public int getSourceRealizableRepairUnitCount() {
        int count = 0;
        for (SourceRepairUnit unit : sourceRepairUnits) {
            if (unit.isSourceRealizable()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasSourceRealizableRepairUnit() {
        return getSourceRealizableRepairUnitCount() > 0;
    }

    public List<SourceRepairPlan> getSourceRepairPlans() {
        return sourceRepairPlans;
    }

    public SourceRepairPlanValidationResult getSourceRepairPlanValidationResult() {
        return sourceRepairPlanValidationResult;
    }

    public List<InferenceLocalizationStudyCandidate> getWeightedMcsCandidates() {
        return weightedMcsCandidates;
    }
}
