package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;

/** Derives conservative source-realizable repair units from inference constraint contexts. */
public final class SourceRepairUnitExtractor {
    public List<SourceRepairUnit> extract(List<InferenceConstraintContext> contexts) {
        List<SourceRepairUnit> units = new ArrayList<>();
        for (InferenceConstraintContext context : contexts) {
            for (InferenceSlotContext slot : context.getSlots()) {
                SourceRepairUnit unit = toRepairUnit(context, slot);
                if (unit != null) {
                    addUnique(units, unit);
                }
            }
        }
        return units;
    }

    private static SourceRepairUnit toRepairUnit(
            InferenceConstraintContext context, InferenceSlotContext slot) {
        if ("VARIABLE".equals(slot.getKind()) && slot.isInsertable()) {
            String location = repairLocation(context, slot);
            if (!location.isEmpty()) {
                return new SourceRepairUnit(
                        "slot#" + slot.getId() + ":" + location,
                        "ANNOTATION_SITE",
                        "INFERRED_ANNOTATION_SLOT",
                        true,
                        location,
                        context.summarize(),
                        annotationEditDomain());
            }
        }
        if (!"CONSTANT".equals(slot.getKind()) && !slot.isInsertable()) {
            String location = expressionRepairLocation(context, slot);
            if (!location.isEmpty()) {
                return new SourceRepairUnit(
                        "expr-slot#" + slot.getId() + ":" + location,
                        "EXPRESSION_SITE",
                        "REFINEMENT_SLOT",
                        true,
                        location,
                        context.summarize(),
                        expressionRepairDomain());
            }
        }
        if ("CONSTANT".equals(slot.getKind())) {
            return new SourceRepairUnit(
                    "constant:" + slot.getDescription() + ":" + context.getLocation(),
                    "LOGICAL_CONSTRAINT",
                    "TYPE_SYSTEM_OR_DECLARED_QUALIFIER",
                    false,
                    context.getLocation(),
                    context.summarize(),
                    identityOnlyDomain());
        }
        return null;
    }

    private static String repairLocation(
            InferenceConstraintContext context, InferenceSlotContext slot) {
        if ("AST_PATH".equals(slot.getLocationKind()) && !slot.getLocation().isEmpty()) {
            return "AST_PATH:" + slot.getLocation();
        }
        if ("AST_PATH".equals(context.getLocationKind()) && !context.getLocation().isEmpty()) {
            return "AST_PATH:" + context.getLocation();
        }
        return "";
    }

    private static String expressionRepairLocation(
            InferenceConstraintContext context, InferenceSlotContext slot) {
        if ("AST_PATH".equals(context.getLocationKind()) && !context.getLocation().isEmpty()) {
            return "AST_PATH:" + context.getLocation();
        }
        if ("AST_PATH".equals(slot.getLocationKind()) && !slot.getLocation().isEmpty()) {
            return "AST_PATH:" + slot.getLocation();
        }
        return "";
    }

    private static List<SourceRepairEditOption> annotationEditDomain() {
        List<SourceRepairEditOption> options = new ArrayList<>();
        options.add(new SourceRepairEditOption("Identity", "", 0));
        options.add(new SourceRepairEditOption("InsertQualifier", "@nninf.qual.Nullable", 1));
        options.add(new SourceRepairEditOption("ReplaceQualifier", "@nninf.qual.Nullable", 2));
        options.add(new SourceRepairEditOption("RemoveQualifier", "", 3));
        return options;
    }

    private static List<SourceRepairEditOption> identityOnlyDomain() {
        List<SourceRepairEditOption> options = new ArrayList<>();
        options.add(new SourceRepairEditOption("Identity", "", 0));
        return options;
    }

    private static List<SourceRepairEditOption> expressionRepairDomain() {
        List<SourceRepairEditOption> options = new ArrayList<>();
        options.add(new SourceRepairEditOption("Identity", "", 0));
        options.add(new SourceRepairEditOption("RepairExpression", "", 1));
        return options;
    }

    private static void addUnique(List<SourceRepairUnit> units, SourceRepairUnit candidate) {
        for (SourceRepairUnit unit : units) {
            if (unit.getId().equals(candidate.getId())) {
                return;
            }
        }
        units.add(candidate);
    }
}
