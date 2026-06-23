import nninf.qual.Nullable;

class NullDerefRepair {
    int length(@Nullable String value) {
        // :: fixable-error: (dereference.of.nullable)
        return value.length();
    }
}
