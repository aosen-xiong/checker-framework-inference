import nninf.qual.Nullable;

class MethodCallRepair {
    void recordId(String id) {}

    void setId(@Nullable String id) {
        // :: fixable-error: (argument.type.incompatible)
        recordId(id);
    }
}
