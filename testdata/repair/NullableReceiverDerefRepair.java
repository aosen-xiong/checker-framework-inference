import nninf.qual.Nullable;

class NullableReceiverDerefRepair {
    private @Nullable Builder builder;

    void add(String name, String value) {
        // :: fixable-error: (dereference.of.nullable)
        builder.add(name, value);
    }

    static final class Builder {
        void add(String name, String value) {}
    }
}
