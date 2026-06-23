package checkers.inference.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.DefaultInferenceResult;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.repair.InferenceConstraintContext;
import checkers.inference.repair.InferenceRepairCandidate;
import checkers.inference.repair.InferenceRepairKind;
import checkers.inference.repair.InferenceConstraintReport;
import checkers.inference.repair.InferenceSlotContext;
import checkers.inference.repair.InferenceSnapshotReporter;
import checkers.inference.repair.SimpleNninfRepairPlanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class InferenceConstraintContextTest {
    @Test
    public void reportsUnsatConstraintSourceContext() {
        AnnotationLocation location = new AnnotationLocation.ClassDeclLocation("repair.Example");
        TestVariableSlot left = new TestVariableSlot(1, location, true);
        TestVariableSlot right = new TestVariableSlot(2, location, false);
        Constraint conflict = EqualityConstraint.create(left, right, location);
        InferenceRunSnapshot snapshot =
                new InferenceRunSnapshot(
                        Arrays.asList(left, right),
                        Collections.singletonList(conflict),
                        new DefaultInferenceResult(Collections.singletonList(conflict)));

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);

        assertFalse(report.solverHadSolution());
        assertEquals(1, report.getUnsatConstraintCount());
        assertEquals(1, report.getUnsatConstraintContexts().size());

        InferenceConstraintContext context = report.getUnsatConstraintContexts().get(0);
        assertEquals("EqualityConstraint", context.getKind());
        assertEquals("slot#1 == slot#2", context.getRelation());
        assertEquals("CLASS_DECL", context.getLocationKind());
        assertTrue(context.getLocation().contains("repair.Example"));
        assertEquals(2, context.getSlots().size());

        InferenceSlotContext firstSlot = context.getSlots().get(0);
        assertEquals(1, firstSlot.getId());
        assertEquals("VARIABLE", firstSlot.getKind());
        assertTrue(firstSlot.isInsertable());
        assertEquals("CLASS_DECL", firstSlot.getLocationKind());
        assertTrue(firstSlot.getLocation().contains("repair.Example"));

        InferenceSlotContext secondSlot = context.getSlots().get(1);
        assertEquals(2, secondSlot.getId());
        assertFalse(secondSlot.isInsertable());

        System.out.println("=== unsat constraint source context example ===");
        System.out.println(context.summarize());
    }

    @Test
    public void plansCandidateFromUnsatConstraintContext() {
        AnnotationLocation location = new AnnotationLocation.ClassDeclLocation("repair.Example");
        TestVariableSlot left = new TestVariableSlot(1, location, true);
        TestVariableSlot right = new TestVariableSlot(2, location, false);
        Constraint conflict = EqualityConstraint.create(left, right, location);
        InferenceRunSnapshot snapshot =
                new InferenceRunSnapshot(
                        Arrays.asList(left, right),
                        Collections.singletonList(conflict),
                        new DefaultInferenceResult(Collections.singletonList(conflict)));

        InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(report.getUnsatConstraintContexts());

        assertEquals(1, candidates.size());
        InferenceRepairCandidate candidate = candidates.get(0);
        assertEquals("slot#1 == slot#2", candidate.getConstraintContext().getRelation());
        assertEquals(1, candidate.getTargetSlot().getId());
        assertEquals("@Nullable", candidate.getQualifier());
        assertEquals("weaken inference slot to @Nullable", candidate.getDescription());

        System.out.println("=== inference-context repair candidate example ===");
        System.out.println(candidate.summarize());
    }

    @Test
    public void ranksSourceExpressionRepairBeforeAnnotationWeakening() {
        InferenceConstraintContext annotationContext =
                new InferenceConstraintContext(
                        "EqualityConstraint",
                        "slot#1 == @Nullable",
                        "CLASS_DECL",
                        "ClassDeclLocation( repair.Example )",
                        Arrays.asList(
                                new InferenceSlotContext(
                                        1,
                                        "VARIABLE",
                                        true,
                                        "CLASS_DECL",
                                        "ClassDeclLocation( repair.Example )",
                                        "slot#1")));
        InferenceConstraintContext expressionContext =
                new InferenceConstraintContext(
                        "InequalityConstraint",
                        "slot#2 ? @Nullable",
                        "AST_PATH",
                        "AstPathLocation( repair.Example.m()V.null:repair.Example:m()V::"
                                + "Method.body, Block.statement 0, ExpressionStatement.expression,"
                                + " Assignment.expression )",
                        Arrays.asList(
                                new InferenceSlotContext(
                                        2,
                                        "REFINEMENT_VARIABLE",
                                        false,
                                        "AST_PATH",
                                        "AstPathLocation( repair.Example.m()V.null:repair.Example:m()V::"
                                                + "Method.body, Block.statement 0 )",
                                        "slot#2")));

        List<InferenceRepairCandidate> candidates =
                new SimpleNninfRepairPlanner()
                        .planFromInferenceContexts(Arrays.asList(annotationContext, expressionContext));

        assertEquals(2, candidates.size());
        assertEquals(InferenceRepairKind.INSERT_NULL_GUARD, candidates.get(0).getRepairKind());
        assertEquals(2, candidates.get(0).getTargetSlot().getId());
        assertEquals(InferenceRepairKind.WEAKEN_ANNOTATION, candidates.get(1).getRepairKind());
        assertEquals(1, candidates.get(1).getTargetSlot().getId());
    }

    private static final class TestVariableSlot extends VariableSlot {
        private final boolean insertable;

        private TestVariableSlot(int id, AnnotationLocation location, boolean insertable) {
            super(id, location);
            this.insertable = insertable;
        }

        @Override
        public Kind getKind() {
            return Kind.VARIABLE;
        }

        @Override
        public boolean isInsertable() {
            return insertable;
        }

        @Override
        public <S, T> S serialize(Serializer<S, T> serializer) {
            throw new UnsupportedOperationException("Test slot is not serialized");
        }
    }
}
