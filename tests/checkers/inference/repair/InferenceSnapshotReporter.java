package checkers.inference.repair;

import checkers.inference.InferenceRunSnapshot;
import checkers.inference.model.AnnotationLocation;
import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return new InferenceConstraintReport(
                snapshot.getSlots().size(),
                insertableSlotCount,
                snapshot.getConstraints().size(),
                locatedConstraintCount,
                constraintCountsByType,
                snapshot.hasSolution(),
                unsatConstraints.size());
    }
}
