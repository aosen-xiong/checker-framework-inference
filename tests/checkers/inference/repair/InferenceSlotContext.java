package checkers.inference.repair;

/** Repair-oriented source context for one inference slot. */
public final class InferenceSlotContext {
    private final int id;
    private final String kind;
    private final boolean insertable;
    private final String locationKind;
    private final String location;
    private final String description;

    public InferenceSlotContext(
            int id,
            String kind,
            boolean insertable,
            String locationKind,
            String location,
            String description) {
        this.id = id;
        this.kind = kind;
        this.insertable = insertable;
        this.locationKind = locationKind;
        this.location = location;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public String getLocationKind() {
        return locationKind;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String summarize() {
        return description
                + "{kind="
                + kind
                + ", insertable="
                + insertable
                + ", locationKind="
                + locationKind
                + ", location="
                + location
                + "}";
    }
}
