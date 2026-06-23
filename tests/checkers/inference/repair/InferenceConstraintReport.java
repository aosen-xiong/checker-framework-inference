package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final List<InferenceConstraintContext> unsatConstraintContexts;
    private final List<InferenceConstraintContext> repairConstraintContexts;

    public InferenceConstraintReport(
            int slotCount,
            int insertableSlotCount,
            int constraintCount,
            int locatedConstraintCount,
            Map<String, Integer> constraintCountsByType,
            boolean solverHadSolution,
            int unsatConstraintCount,
            List<InferenceConstraintContext> unsatConstraintContexts,
            List<InferenceConstraintContext> repairConstraintContexts) {
        this.slotCount = slotCount;
        this.insertableSlotCount = insertableSlotCount;
        this.constraintCount = constraintCount;
        this.locatedConstraintCount = locatedConstraintCount;
        this.constraintCountsByType =
                Collections.unmodifiableMap(new LinkedHashMap<>(constraintCountsByType));
        this.solverHadSolution = solverHadSolution;
        this.unsatConstraintCount = unsatConstraintCount;
        this.unsatConstraintContexts =
                Collections.unmodifiableList(new ArrayList<>(unsatConstraintContexts));
        this.repairConstraintContexts =
                Collections.unmodifiableList(new ArrayList<>(repairConstraintContexts));
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

    public List<InferenceConstraintContext> getUnsatConstraintContexts() {
        return unsatConstraintContexts;
    }

    public List<InferenceConstraintContext> getRepairConstraintContexts() {
        return repairConstraintContexts;
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
                + ", unsatConstraintContexts="
                + summarizeUnsatContexts()
                + ", repairConstraintContexts="
                + summarizeRepairContexts()
                + ", byType="
                + constraintCountsByType;
    }

    private List<String> summarizeUnsatContexts() {
        List<String> summaries = new ArrayList<>();
        for (InferenceConstraintContext context : unsatConstraintContexts) {
            summaries.add(context.summarize());
        }
        return summaries;
    }

    private List<String> summarizeRepairContexts() {
        List<String> summaries = new ArrayList<>();
        for (InferenceConstraintContext context : repairConstraintContexts) {
            summaries.add(context.summarize());
        }
        return summaries;
    }
}
