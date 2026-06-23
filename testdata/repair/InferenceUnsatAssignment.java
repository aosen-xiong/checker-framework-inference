import nninf.qual.NonNull;
import nninf.qual.Nullable;

class InferenceUnsatAssignment {
    void setId(@Nullable String maybeId) {
        @NonNull String id = maybeId;
        id.toString();
    }
}
