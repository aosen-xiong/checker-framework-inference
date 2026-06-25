package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Metadata and results for one bounded MCS enumeration run. */
public final class InferenceMcsEnumerationResult {
    private final int originalUniverseSize;
    private final int enumeratedUniverseSize;
    private final boolean universeTruncated;
    private final int maxRemovalSize;
    private final boolean searchBounded;
    private final List<InferenceMcsResult> results;

    public InferenceMcsEnumerationResult(
            int originalUniverseSize,
            int enumeratedUniverseSize,
            boolean universeTruncated,
            int maxRemovalSize,
            boolean searchBounded,
            List<InferenceMcsResult> results) {
        this.originalUniverseSize = originalUniverseSize;
        this.enumeratedUniverseSize = enumeratedUniverseSize;
        this.universeTruncated = universeTruncated;
        this.maxRemovalSize = maxRemovalSize;
        this.searchBounded = searchBounded;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    public int getOriginalUniverseSize() {
        return originalUniverseSize;
    }

    public int getEnumeratedUniverseSize() {
        return enumeratedUniverseSize;
    }

    public boolean isUniverseTruncated() {
        return universeTruncated;
    }

    public int getMaxRemovalSize() {
        return maxRemovalSize;
    }

    public boolean isSearchBounded() {
        return searchBounded;
    }

    public List<InferenceMcsResult> getResults() {
        return results;
    }
}
