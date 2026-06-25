package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A minimal correction set over inference constraint contexts. */
public final class InferenceMcsResult {
    private final int rank;
    private final int weight;
    private final List<InferenceConstraintContext> removedContexts;

    public InferenceMcsResult(
            int rank, int weight, List<InferenceConstraintContext> removedContexts) {
        this.rank = rank;
        this.weight = weight;
        this.removedContexts =
                Collections.unmodifiableList(new ArrayList<>(removedContexts));
    }

    public int getRank() {
        return rank;
    }

    public int getWeight() {
        return weight;
    }

    public List<InferenceConstraintContext> getRemovedContexts() {
        return removedContexts;
    }
}
