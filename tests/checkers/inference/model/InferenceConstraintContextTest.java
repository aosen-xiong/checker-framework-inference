package checkers.inference.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.DefaultInferenceResult;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.repair.InferenceConstraintContext;
import checkers.inference.repair.InferenceRepairCandidate;
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
