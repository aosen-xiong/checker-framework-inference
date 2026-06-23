import nninf.qual.NonNull;
import nninf.qual.Nullable;

class InferenceUnsatFieldAssignment {
    @NonNull String id = "";

    void setId(@Nullable String maybeId) {
        id = maybeId;
        id.toString();
    }
}
