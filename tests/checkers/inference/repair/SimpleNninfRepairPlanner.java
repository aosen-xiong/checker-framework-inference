package checkers.inference.repair;

import static checkers.inference.repair.SimpleNninfUnsatCoreSolver.NULLABLE;

import java.util.ArrayList;
import java.util.List;

/** Proposes local nninf repairs from unsat-core constraints. */
public final class SimpleNninfRepairPlanner {
    public List<RepairCandidate> plan(UnsatCoreResult coreResult) {
        List<RepairCandidate> candidates = new ArrayList<>();
        for (RepairConstraint constraint : coreResult.getUnsatCore()) {
            candidates.add(
                    new RepairCandidate(
                            constraint,
                            constraint.getSupertype(),
                            NULLABLE,
                            "weaken target slot to @Nullable"));
        }
        return candidates;
    }

    public List<InferenceRepairCandidate> planFromInferenceContexts(
            List<InferenceConstraintContext> contexts) {
        List<InferenceRepairCandidate> candidates = new ArrayList<>();
        for (InferenceConstraintContext context : contexts) {
            InferenceSlotContext targetSlot = firstInsertableVariableSlot(context);
            if (targetSlot != null) {
                candidates.add(
                        new InferenceRepairCandidate(
                                context,
                                targetSlot,
                                NULLABLE,
                                "weaken inference slot to @Nullable"));
            }
        }
        return candidates;
    }

    private static InferenceSlotContext firstInsertableVariableSlot(
            InferenceConstraintContext context) {
        for (InferenceSlotContext slot : context.getSlots()) {
            if (slot.isInsertable() && "VARIABLE".equals(slot.getKind())) {
                return slot;
            }
        }
        return null;
    }
}
