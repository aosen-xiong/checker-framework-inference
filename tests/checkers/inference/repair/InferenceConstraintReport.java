package checkers.inference.repair;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Summary of real constraints produced by one inference run. */
public final class InferenceConstraintReport {
    private final int slotCount;
    private final int insertableSlotCount;
    private final int constraintCount;
    private final int locatedConstraintCount;
    private final Map<String, Integer> constraintCountsByType;
    private final boolean solverHadSolution;
    private final int unsatConstraintCount;

    public InferenceConstraintReport(
            int slotCount,
            int insertableSlotCount,
            int constraintCount,
            int locatedConstraintCount,
            Map<String, Integer> constraintCountsByType,
            boolean solverHadSolution,
            int unsatConstraintCount) {
        this.slotCount = slotCount;
        this.insertableSlotCount = insertableSlotCount;
        this.constraintCount = constraintCount;
        this.locatedConstraintCount = locatedConstraintCount;
        this.constraintCountsByType =
                Collections.unmodifiableMap(new LinkedHashMap<>(constraintCountsByType));
        this.solverHadSolution = solverHadSolution;
        this.unsatConstraintCount = unsatConstraintCount;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public int getInsertableSlotCount() {
        return insertableSlotCount;
    }

    public int getConstraintCount() {
        return constraintCount;
    }

    public int getLocatedConstraintCount() {
        return locatedConstraintCount;
    }

    public Map<String, Integer> getConstraintCountsByType() {
        return constraintCountsByType;
    }

    public boolean solverHadSolution() {
        return solverHadSolution;
    }

    public int getUnsatConstraintCount() {
        return unsatConstraintCount;
    }

    public String summarize() {
        return "slots="
                + slotCount
                + ", insertableSlots="
                + insertableSlotCount
                + ", constraints="
                + constraintCount
                + ", locatedConstraints="
                + locatedConstraintCount
                + ", solverHadSolution="
                + solverHadSolution
                + ", unsatConstraints="
                + unsatConstraintCount
                + ", byType="
                + constraintCountsByType;
    }
}
