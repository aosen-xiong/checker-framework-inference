package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Combines deterministic, checker-specific, and AI-backed edit providers in priority order. */
public final class CompositeInferenceRepairEditProvider implements InferenceRepairEditProvider {
    private final List<InferenceRepairEditProvider> providers;

    public CompositeInferenceRepairEditProvider(List<InferenceRepairEditProvider> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
    }

    @Override
    public List<InferenceRepairEdit> generate(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource) {
        List<InferenceRepairEdit> edits = new ArrayList<>();
        for (InferenceRepairEditProvider provider : providers) {
            for (InferenceRepairEdit edit : provider.generate(candidate, target, originalSource)) {
                if (!containsEquivalentEdit(edits, edit)) {
                    edits.add(edit);
                }
            }
        }
        return edits;
    }

    private static boolean containsEquivalentEdit(
            List<InferenceRepairEdit> edits, InferenceRepairEdit candidate) {
        for (InferenceRepairEdit edit : edits) {
            if (edit.getRepairKind() == candidate.getRepairKind()
                    && edit.getReplacementSource().equals(candidate.getReplacementSource())) {
                return true;
            }
        }
        return false;
    }
}
