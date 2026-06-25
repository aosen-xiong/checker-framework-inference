package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InferenceMcsEnumeratorTest {
    @Test
    public void enumeratesSingletonMcsForOneReportedCore() {
        InferenceConstraintContext left = context("left");
        InferenceConstraintContext right = context("right");

        List<InferenceMcsResult> results =
                new InferenceMcsEnumerator()
                        .enumerateTopK(
                                Arrays.asList(left, right),
                                new ReportedCoreSatisfiabilityOracle(Arrays.asList(left, right)),
                                5);

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getRemovedContexts().size());
        assertEquals(1, results.get(1).getRemovedContexts().size());
    }

    @Test
    public void enumeratesMinimalCorrectionSetsAcrossMultipleConflictGroups() {
        InferenceConstraintContext a = context("a");
        InferenceConstraintContext b = context("b");
        InferenceConstraintContext c = context("c");
        InferenceConstraintContext d = context("d");

        List<InferenceMcsResult> results =
                new InferenceMcsEnumerator()
                        .enumerateTopK(
                                Arrays.asList(a, b, c, d),
                                new ConflictGroupOracle(
                                        Arrays.asList(
                                                set(a, b),
                                                set(c, d))),
                                10);

        assertEquals(4, results.size());
        for (InferenceMcsResult result : results) {
            assertEquals(2, result.getRemovedContexts().size());
            Set<String> removed = summaries(result.getRemovedContexts());
            assertTrue(removed.contains(a.summarize()) || removed.contains(b.summarize()));
            assertTrue(removed.contains(c.summarize()) || removed.contains(d.summarize()));
        }
    }

    private static InferenceConstraintContext context(String location) {
        return new InferenceConstraintContext(
                "SubtypeConstraint",
                "slot ? @Nullable",
                "AST_PATH",
                location,
                Collections.<InferenceSlotContext>emptyList());
    }

    private static Set<String> set(InferenceConstraintContext... contexts) {
        return summaries(Arrays.asList(contexts));
    }

    private static Set<String> summaries(List<InferenceConstraintContext> contexts) {
        Set<String> summaries = new HashSet<>();
        for (InferenceConstraintContext context : contexts) {
            summaries.add(context.summarize());
        }
        return summaries;
    }

    private static final class ConflictGroupOracle
            implements InferenceConstraintSatisfiabilityOracle {
        private final List<Set<String>> conflictGroups;

        private ConflictGroupOracle(List<Set<String>> conflictGroups) {
            this.conflictGroups = conflictGroups;
        }

        @Override
        public boolean isSatisfiable(List<InferenceConstraintContext> retainedContexts) {
            Set<String> retained = summaries(retainedContexts);
            for (Set<String> conflictGroup : conflictGroups) {
                if (retained.containsAll(conflictGroup)) {
                    return false;
                }
            }
            return true;
        }
    }
}
