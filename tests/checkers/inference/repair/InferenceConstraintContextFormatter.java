package checkers.inference.repair;

import checkers.inference.model.AnnotationLocation;
import checkers.inference.model.BinaryConstraint;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.model.VariableSlot;

import java.util.ArrayList;
import java.util.List;

/** Formats inference constraints into the source context needed by repair search. */
public final class InferenceConstraintContextFormatter {
    private InferenceConstraintContextFormatter() {}

    public static InferenceConstraintContext format(Constraint constraint) {
        List<InferenceSlotContext> slotContexts = new ArrayList<>();
        for (Slot slot : constraint.getSlots()) {
            slotContexts.add(formatSlot(slot));
        }

        AnnotationLocation location = constraint.getLocation();
        return new InferenceConstraintContext(
                constraint.getClass().getSimpleName(),
                relation(constraint),
                locationKind(location),
                locationDescription(location),
                slotContexts);
    }

    private static InferenceSlotContext formatSlot(Slot slot) {
        AnnotationLocation location =
                slot instanceof VariableSlot
                        ? ((VariableSlot) slot).getLocation()
                        : AnnotationLocation.MISSING_LOCATION;
        return new InferenceSlotContext(
                slot.getId(),
                slot.getKind().name(),
                slot.isInsertable(),
                locationKind(location),
                locationDescription(location),
                slotDescription(slot));
    }

    private static String relation(Constraint constraint) {
        if (constraint instanceof SubtypeConstraint) {
            SubtypeConstraint subtypeConstraint = (SubtypeConstraint) constraint;
            return slotDescription(subtypeConstraint.getSubtype())
                    + " <: "
                    + slotDescription(subtypeConstraint.getSupertype());
        }
        if (constraint instanceof EqualityConstraint) {
            EqualityConstraint equalityConstraint = (EqualityConstraint) constraint;
            return slotDescription(equalityConstraint.getFirst())
                    + " == "
                    + slotDescription(equalityConstraint.getSecond());
        }
        if (constraint instanceof BinaryConstraint) {
            BinaryConstraint binaryConstraint = (BinaryConstraint) constraint;
            return slotDescription(binaryConstraint.getFirst())
                    + " ? "
                    + slotDescription(binaryConstraint.getSecond());
        }
        return constraint.toString();
    }

    private static String slotDescription(Slot slot) {
        if (slot instanceof ConstantSlot) {
            return ((ConstantSlot) slot).getValue().toString();
        }
        return "slot#" + slot.getId();
    }

    private static String locationKind(AnnotationLocation location) {
        return location == null ? "UNKNOWN" : location.getKind().name();
    }

    private static String locationDescription(AnnotationLocation location) {
        return location == null ? "unknown" : location.toString();
    }
}
