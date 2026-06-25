package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SourceRepairUnitExtractorTest {
    @Test
    public void extractsSourceRealizableAnnotationUnitForInsertableAstSlot() {
        InferenceConstraintContext context =
                new InferenceConstraintContext(
                        "SubtypeConstraint",
                        "slot#1 <: @Nullable",
                        "AST_PATH",
                        "AstPathLocation(method body)",
                        Collections.singletonList(
                                new InferenceSlotContext(
                                        1,
                                        "VARIABLE",
                                        true,
                                        "AST_PATH",
                                        "AstPathLocation(parameter)",
                                        "slot#1")));

        List<SourceRepairUnit> units =
                new SourceRepairUnitExtractor().extract(Collections.singletonList(context));

        assertEquals(1, units.size());
        SourceRepairUnit unit = units.get(0);
        assertTrue(unit.isSourceRealizable());
        assertEquals("ANNOTATION_SITE", unit.getKind());
        assertEquals("INFERRED_ANNOTATION_SLOT", unit.getProvenanceKind());
        assertTrue(unit.getLocation().contains("AstPathLocation(parameter)"));
        assertEquals(4, unit.getEditDomain().size());
        assertEquals("InsertQualifier", unit.getEditDomain().get(1).getKind());
    }

    @Test
    public void keepsConstantEvidenceNonRealizable() {
        InferenceConstraintContext context =
                new InferenceConstraintContext(
                        "EqualityConstraint",
                        "@Nullable == slot#1",
                        "AST_PATH",
                        "AstPathLocation(call)",
                        Arrays.asList(
                                new InferenceSlotContext(
                                        -1,
                                        "CONSTANT",
                                        false,
                                        "MISSING",
                                        "",
                                        "@nninf.qual.Nullable")));

        List<SourceRepairUnit> units =
                new SourceRepairUnitExtractor().extract(Collections.singletonList(context));

        assertEquals(1, units.size());
        SourceRepairUnit unit = units.get(0);
        assertFalse(unit.isSourceRealizable());
        assertEquals("LOGICAL_CONSTRAINT", unit.getKind());
        assertEquals("TYPE_SYSTEM_OR_DECLARED_QUALIFIER", unit.getProvenanceKind());
        assertEquals(1, unit.getEditDomain().size());
        assertEquals("Identity", unit.getEditDomain().get(0).getKind());
    }

    @Test
    public void extractsSourceRealizableExpressionUnitForRefinementAstSlot() {
        InferenceConstraintContext context =
                new InferenceConstraintContext(
                        "InequalityConstraint",
                        "slot#7 <: @Nullable",
                        "AST_PATH",
                        "AstPathLocation(method body expression)",
                        Collections.singletonList(
                                new InferenceSlotContext(
                                        7,
                                        "REFINEMENT_VARIABLE",
                                        false,
                                        "AST_PATH",
                                        "AstPathLocation(local initializer)",
                                        "slot#7")));

        List<SourceRepairUnit> units =
                new SourceRepairUnitExtractor().extract(Collections.singletonList(context));

        assertEquals(1, units.size());
        SourceRepairUnit unit = units.get(0);
        assertTrue(unit.isSourceRealizable());
        assertEquals("EXPRESSION_SITE", unit.getKind());
        assertEquals("REFINEMENT_SLOT", unit.getProvenanceKind());
        assertTrue(unit.getLocation().contains("method body expression"));
        assertEquals(2, unit.getEditDomain().size());
        assertEquals("RepairExpression", unit.getEditDomain().get(1).getKind());
    }
}
