import nninf.qual.Nullable;

class AssignmentRepair {
    private String id = "1234";

    void setId(@Nullable String id) {
        // :: fixable-error: (assignment.type.incompatible)
        this.id = id;
    }
}
