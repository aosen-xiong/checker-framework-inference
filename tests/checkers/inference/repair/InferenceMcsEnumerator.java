package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Enumerates bounded top-k minimal correction sets for inference constraint contexts. */
public final class InferenceMcsEnumerator {
    private static final int DEFAULT_MAX_UNIVERSE_SIZE = 18;
    private static final int DEFAULT_MAX_REMOVAL_SIZE = 3;

    private final int maxUniverseSize;
    private final int maxRemovalSize;

    public InferenceMcsEnumerator() {
        this(DEFAULT_MAX_UNIVERSE_SIZE, DEFAULT_MAX_REMOVAL_SIZE);
    }

    public InferenceMcsEnumerator(int maxUniverseSize) {
        this(maxUniverseSize, DEFAULT_MAX_REMOVAL_SIZE);
    }

    public InferenceMcsEnumerator(int maxUniverseSize, int maxRemovalSize) {
        this.maxUniverseSize = maxUniverseSize;
        this.maxRemovalSize = maxRemovalSize;
    }

    public List<InferenceMcsResult> enumerateTopK(
            List<InferenceConstraintContext> contexts,
            InferenceConstraintSatisfiabilityOracle oracle,
            int topK) {
        return enumerateTopKWithMetadata(contexts, oracle, topK).getResults();
    }

    public InferenceMcsEnumerationResult enumerateTopKWithMetadata(
            List<InferenceConstraintContext> contexts,
            InferenceConstraintSatisfiabilityOracle oracle,
            int topK) {
        List<InferenceConstraintContext> universe = uniqueContexts(contexts);
        int originalUniverseSize = universe.size();
        boolean universeTruncated = universe.size() > maxUniverseSize;
        if (topK <= 0 || universe.isEmpty() || oracle.isSatisfiable(universe)) {
            return new InferenceMcsEnumerationResult(
                    originalUniverseSize,
                    Math.min(originalUniverseSize, maxUniverseSize),
                    universeTruncated,
                    maxRemovalSize,
                    false,
                    Collections.<InferenceMcsResult>emptyList());
        }
        if (universeTruncated) {
            universe = universe.subList(0, maxUniverseSize);
        }

        List<Candidate> minimalCorrectionSets = new ArrayList<>();
        int largestSubsetSize = Math.min(universe.size(), maxRemovalSize);
        for (int subsetSize = 1; subsetSize <= largestSubsetSize; subsetSize++) {
            enumerateSubsets(
                    universe,
                    oracle,
                    subsetSize,
                    0,
                    new ArrayList<Integer>(),
                    minimalCorrectionSets);
            if (minimalCorrectionSets.size() >= topK) {
                break;
            }
        }

        Collections.sort(
                minimalCorrectionSets,
                new Comparator<Candidate>() {
                    @Override
                    public int compare(Candidate left, Candidate right) {
                        if (left.weight != right.weight) {
                            return left.weight - right.weight;
                        }
                        if (left.indexes.size() != right.indexes.size()) {
                            return left.indexes.size() - right.indexes.size();
                        }
                        return left.key.compareTo(right.key);
                    }
                });

        List<InferenceMcsResult> results = new ArrayList<>();
        int rank = 1;
        for (Candidate candidate : minimalCorrectionSets) {
            if (rank > topK) {
                break;
            }
            results.add(new InferenceMcsResult(rank, candidate.weight, removed(universe, candidate.indexes)));
            rank++;
        }
        return new InferenceMcsEnumerationResult(
                originalUniverseSize,
                universe.size(),
                universeTruncated,
                maxRemovalSize,
                largestSubsetSize < universe.size() && results.size() < topK,
                results);
    }

    private void enumerateSubsets(
            List<InferenceConstraintContext> universe,
            InferenceConstraintSatisfiabilityOracle oracle,
            int targetSize,
            int startIndex,
            List<Integer> current,
            List<Candidate> minimalCorrectionSets) {
        if (current.size() == targetSize) {
            Candidate candidate = candidateIfMinimalCorrectionSet(universe, oracle, current);
            if (candidate != null && !containsEquivalent(minimalCorrectionSets, candidate)) {
                minimalCorrectionSets.add(candidate);
            }
            return;
        }
        for (int index = startIndex; index < universe.size(); index++) {
            current.add(index);
            enumerateSubsets(
                    universe,
                    oracle,
                    targetSize,
                    index + 1,
                    current,
                    minimalCorrectionSets);
            current.remove(current.size() - 1);
        }
    }

    private Candidate candidateIfMinimalCorrectionSet(
            List<InferenceConstraintContext> universe,
            InferenceConstraintSatisfiabilityOracle oracle,
            List<Integer> removedIndexes) {
        if (!oracle.isSatisfiable(retained(universe, removedIndexes))) {
            return null;
        }
        for (int index = 0; index < removedIndexes.size(); index++) {
            List<Integer> smallerRemoval = new ArrayList<>(removedIndexes);
            smallerRemoval.remove(index);
            if (oracle.isSatisfiable(retained(universe, smallerRemoval))) {
                return null;
            }
        }
        return new Candidate(new ArrayList<>(removedIndexes), weight(universe, removedIndexes));
    }

    private static List<InferenceConstraintContext> retained(
            List<InferenceConstraintContext> universe, List<Integer> removedIndexes) {
        Set<Integer> removed = new HashSet<>(removedIndexes);
        List<InferenceConstraintContext> retained = new ArrayList<>();
        for (int index = 0; index < universe.size(); index++) {
            if (!removed.contains(index)) {
                retained.add(universe.get(index));
            }
        }
        return retained;
    }

    private static List<InferenceConstraintContext> removed(
            List<InferenceConstraintContext> universe, List<Integer> removedIndexes) {
        List<InferenceConstraintContext> removed = new ArrayList<>();
        for (Integer index : removedIndexes) {
            removed.add(universe.get(index));
        }
        return removed;
    }

    private static int weight(
            List<InferenceConstraintContext> universe, List<Integer> removedIndexes) {
        int weight = 0;
        for (Integer index : removedIndexes) {
            weight += contextWeight(universe.get(index));
        }
        return weight;
    }

    static int contextWeight(InferenceConstraintContext context) {
        String location = locationKey(context);
        int weight = 10;
        if (location.startsWith("AST_PATH:")) {
            weight -= 3;
        }
        if (location.contains("MethodInvocation.argument")) {
            weight -= 2;
        }
        if (location.contains("ExpressionStatement.expression")) {
            weight -= 1;
        }
        if (location.contains("MemberSelect.expression")
                || location.contains("MethodInvocation.methodSelect")) {
            weight += 2;
        }
        if (context.getKind().contains("Subtype")
                || context.getKind().contains("Inequality")) {
            weight -= 1;
        }
        return Math.max(1, weight);
    }

    static String locationKey(InferenceConstraintContext context) {
        if (context.getLocation() != null && !context.getLocation().isEmpty()) {
            return context.getLocationKind() + ":" + context.getLocation();
        }
        for (InferenceSlotContext slot : context.getSlots()) {
            if (slot.getLocation() != null && !slot.getLocation().isEmpty()) {
                return slot.getLocationKind() + ":" + slot.getLocation();
            }
        }
        return "";
    }

    private static List<InferenceConstraintContext> uniqueContexts(
            List<InferenceConstraintContext> contexts) {
        List<InferenceConstraintContext> unique = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (InferenceConstraintContext context : contexts) {
            String key = context.summarize();
            if (keys.add(key)) {
                unique.add(context);
            }
        }
        return unique;
    }

    private static boolean containsEquivalent(List<Candidate> candidates, Candidate candidate) {
        for (Candidate existing : candidates) {
            if (existing.key.equals(candidate.key)) {
                return true;
            }
        }
        return false;
    }

    private static final class Candidate {
        private final List<Integer> indexes;
        private final int weight;
        private final String key;

        private Candidate(List<Integer> indexes, int weight) {
            this.indexes = indexes;
            this.weight = weight;
            this.key = indexes.toString();
        }
    }
}
