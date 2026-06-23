package checkers.inference.repair;

import static checkers.inference.repair.SimpleNninfUnsatCoreSolver.NULLABLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                                InferenceRepairKind.WEAKEN_ANNOTATION,
                                NULLABLE,
                                "weaken inference slot to @Nullable"));
            }

            targetSlot = firstSourceLocatedInferenceSlot(context);
            if (targetSlot != null) {
                candidates.add(
                        new InferenceRepairCandidate(
                                context,
                                sourceRepairTargetSlot(context, targetSlot),
                                InferenceRepairKind.INSERT_NULL_GUARD,
                                NULLABLE,
                                "repair source expression causing @Nullable conflict"));
            }
        }
        Collections.sort(candidates, new InferenceRepairCandidateComparator());
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

    private static InferenceSlotContext sourceRepairTargetSlot(
            InferenceConstraintContext context, InferenceSlotContext slot) {
        if ("AST_PATH".equals(context.getLocationKind())) {
            return new InferenceSlotContext(
                    slot.getId(),
                    slot.getKind(),
                    slot.isInsertable(),
                    context.getLocationKind(),
                    context.getLocation(),
                    slot.getDescription());
        }
        return slot;
    }

    private static InferenceSlotContext firstSourceLocatedInferenceSlot(
            InferenceConstraintContext context) {
        for (InferenceSlotContext slot : context.getSlots()) {
            if (!"CONSTANT".equals(slot.getKind()) && "AST_PATH".equals(slot.getLocationKind())) {
                return slot;
            }
        }
        return null;
    }

    private static final class InferenceRepairCandidateComparator
            implements Comparator<InferenceRepairCandidate> {
        @Override
        public int compare(InferenceRepairCandidate left, InferenceRepairCandidate right) {
            int leftRank = rank(left);
            int rightRank = rank(right);
            if (leftRank != rightRank) {
                return leftRank - rightRank;
            }
            return left.getTargetSlot().getId() - right.getTargetSlot().getId();
        }

        private static int rank(InferenceRepairCandidate candidate) {
            int sourceRank = sourceLocationRank(candidate);
            if (candidate.getRepairKind() == InferenceRepairKind.INSERT_NULL_GUARD) {
                return sourceRank;
            }
            if (candidate.getRepairKind() == InferenceRepairKind.WEAKEN_ANNOTATION) {
                return 100 + sourceRank;
            }
            return 200 + sourceRank;
        }

        private static int sourceLocationRank(InferenceRepairCandidate candidate) {
            String location = candidate.getTargetSlot().getLocation();
            if (location.contains("MethodInvocation.argument")) {
                return 0;
            }
            if (location.contains("ExpressionStatement.expression")
                    && !location.contains("MethodInvocation.methodSelect")) {
                return 1;
            }
            if (location.contains("MethodInvocation.methodSelect")
                    || location.contains("MemberSelect.expression")) {
                return 20;
            }
            return 10;
        }
    }
}
