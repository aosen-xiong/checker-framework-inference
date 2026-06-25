package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CompositeInferenceRepairEditProviderTest {
    @Test
    public void combinesProvidersInPriorityOrder() {
        CompositeInferenceRepairEditProvider provider =
                new CompositeInferenceRepairEditProvider(
                        Arrays.<InferenceRepairEditProvider>asList(
                                new FixedEditProvider("deterministicFallback"),
                                new FixedAiEditProvider("aiFallback")));

        List<InferenceRepairEdit> edits = provider.generate(null, target(), "class Test {}");

        assertEquals(2, edits.size());
        assertEquals("deterministicFallback", edits.get(0).getReplacementSource());
        assertEquals("aiFallback", edits.get(1).getReplacementSource());
    }

    @Test
    public void removesDuplicateProviderEdits() {
        CompositeInferenceRepairEditProvider provider =
                new CompositeInferenceRepairEditProvider(
                        Arrays.<InferenceRepairEditProvider>asList(
                                new FixedEditProvider("fallback"),
                                new FixedAiEditProvider("fallback")));

        List<InferenceRepairEdit> edits = provider.generate(null, target(), "class Test {}");

        assertEquals(1, edits.size());
        assertEquals("fallback", edits.get(0).getReplacementSource());
    }

    private static InferenceRepairTarget target() {
        return new InferenceRepairTarget(null, "IDENTIFIER", 0, 1, 1, 1, "value");
    }

    private static final class FixedEditProvider implements InferenceRepairEditProvider {
        private final String replacementSource;

        private FixedEditProvider(String replacementSource) {
            this.replacementSource = replacementSource;
        }

        @Override
        public List<InferenceRepairEdit> generate(
                InferenceRepairCandidate candidate,
                InferenceRepairTarget target,
                String originalSource) {
            return Collections.singletonList(edit(replacementSource));
        }
    }

    private static final class FixedAiEditProvider implements AiInferenceRepairEditProvider {
        private final String replacementSource;

        private FixedAiEditProvider(String replacementSource) {
            this.replacementSource = replacementSource;
        }

        @Override
        public List<InferenceRepairEdit> generate(RepairPromptContext context) {
            return Collections.singletonList(edit(replacementSource));
        }
    }

    private static InferenceRepairEdit edit(String replacementSource) {
        return new InferenceRepairEdit(
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                replacementSource,
                "test edit",
                "test_edit");
    }
}
