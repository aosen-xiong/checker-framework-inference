package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;
import checkers.inference.model.AnnotationLocation;
import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates repair-oriented summaries from real inference snapshots. */
public final class InferenceSnapshotReporter {
    private InferenceSnapshotReporter() {}

    public static InferenceConstraintReport report(InferenceRunSnapshot snapshot) {
        int insertableSlotCount = 0;
        for (Slot slot : snapshot.getSlots()) {
            if (slot.isInsertable()) {
                insertableSlotCount++;
            }
        }

        int locatedConstraintCount = 0;
        Map<String, Integer> constraintCountsByType = new LinkedHashMap<>();
        for (Constraint constraint : snapshot.getConstraints()) {
            if (constraint.getLocation() != null
                    && constraint.getLocation().getKind() != AnnotationLocation.Kind.MISSING) {
                locatedConstraintCount++;
            }
            String type = constraint.getClass().getSimpleName();
            constraintCountsByType.put(type, constraintCountsByType.getOrDefault(type, 0) + 1);
        }

        Collection<Constraint> unsatConstraints = snapshot.getUnsatisfiableConstraints();
        List<InferenceConstraintContext> unsatConstraintContexts = new ArrayList<>();
        for (Constraint constraint : unsatConstraints) {
            unsatConstraintContexts.add(InferenceConstraintContextFormatter.format(constraint));
        }
        List<InferenceConstraintContext> repairConstraintContexts =
                repairConstraintContexts(snapshot.getConstraints(), unsatConstraints);
        return new InferenceConstraintReport(
                snapshot.getSlots().size(),
                insertableSlotCount,
                snapshot.getConstraints().size(),
                locatedConstraintCount,
                constraintCountsByType,
                snapshot.hasSolution(),
                unsatConstraints.size(),
                unsatConstraintContexts,
                repairConstraintContexts);
    }

    private static List<InferenceConstraintContext> repairConstraintContexts(
            Collection<Constraint> constraints, Collection<Constraint> unsatConstraints) {
        Set<Integer> unsatSlotIds = slotIds(unsatConstraints);
        List<InferenceConstraintContext> contexts = new ArrayList<>();
        for (Constraint constraint : constraints) {
            if (unsatConstraints.contains(constraint) || touchesAnySlot(constraint, unsatSlotIds)) {
                contexts.add(InferenceConstraintContextFormatter.format(constraint));
            }
        }
        return contexts;
    }

    private static Set<Integer> slotIds(Collection<Constraint> constraints) {
        Set<Integer> slotIds = new HashSet<>();
        for (Constraint constraint : constraints) {
            for (Slot slot : constraint.getSlots()) {
                slotIds.add(slot.getId());
            }
        }
        return slotIds;
    }

    private static boolean touchesAnySlot(Constraint constraint, Set<Integer> slotIds) {
        for (Slot slot : constraint.getSlots()) {
            if (slotIds.contains(slot.getId())) {
                return true;
            }
        }
        return false;
    }
}
