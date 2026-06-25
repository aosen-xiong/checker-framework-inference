package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One ranked source-location candidate for the localization pilot study. */
public final class InferenceLocalizationStudyCandidate {
    private final int rank;
    private final int weight;
    private final int sourceRepairUnitCount;
    private final int sourceRealizableRepairUnitCount;
    private final List<String> locations;
    private final List<String> evidence;

    public InferenceLocalizationStudyCandidate(
            int rank,
            int weight,
            int sourceRepairUnitCount,
            int sourceRealizableRepairUnitCount,
            List<String> locations,
            List<String> evidence) {
        this.rank = rank;
        this.weight = weight;
        this.sourceRepairUnitCount = sourceRepairUnitCount;
        this.sourceRealizableRepairUnitCount = sourceRealizableRepairUnitCount;
        this.locations = Collections.unmodifiableList(new ArrayList<>(locations));
        this.evidence = Collections.unmodifiableList(new ArrayList<>(evidence));
    }

    public int getRank() {
        return rank;
    }

    public int getWeight() {
        return weight;
    }

    public int getSourceRepairUnitCount() {
        return sourceRepairUnitCount;
    }

    public int getSourceRealizableRepairUnitCount() {
        return sourceRealizableRepairUnitCount;
    }

    public boolean hasSourceRealizableRepairUnit() {
        return sourceRealizableRepairUnitCount > 0;
    }

    public List<String> getLocations() {
        return locations;
    }

    public List<String> getEvidence() {
        return evidence;
    }
}
