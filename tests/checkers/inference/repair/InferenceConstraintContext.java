package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Repair-oriented source context for one inference constraint. */
public final class InferenceConstraintContext {
    private final String kind;
    private final String relation;
    private final String locationKind;
    private final String location;
    private final List<InferenceSlotContext> slots;

    public InferenceConstraintContext(
            String kind,
            String relation,
            String locationKind,
            String location,
            List<InferenceSlotContext> slots) {
        this.kind = kind;
        this.relation = relation;
        this.locationKind = locationKind;
        this.location = location;
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    public String getKind() {
        return kind;
    }

    public String getRelation() {
        return relation;
    }

    public String getLocationKind() {
        return locationKind;
    }

    public String getLocation() {
        return location;
    }

    public List<InferenceSlotContext> getSlots() {
        return slots;
    }

    public String summarize() {
        return kind
                + "("
                + relation
                + ")"
                + "{locationKind="
                + locationKind
                + ", location="
                + location
                + ", slots="
                + slotSummaries()
                + "}";
    }

    private List<String> slotSummaries() {
        List<String> summaries = new ArrayList<>();
        for (InferenceSlotContext slot : slots) {
            summaries.add(slot.summarize());
        }
        return summaries;
    }
}
