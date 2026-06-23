import nninf.qual.Nullable;

class InferenceUnsatMethodCall {
    void recordId(String id) {
        id.toString();
    }

    void setId(@Nullable String maybeId) {
        recordId(maybeId);
    }
}
