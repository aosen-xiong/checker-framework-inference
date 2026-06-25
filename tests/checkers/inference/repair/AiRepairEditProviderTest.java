package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class AiRepairEditProviderTest {
    @Test
    public void convertsAiReplacementStringsToRepairEdits() {
        CapturingClient client = new CapturingClient("fallbackId", "fallbackId", "maybeId", " ");
        AiRepairEditProvider provider = new AiRepairEditProvider(client);

        List<InferenceRepairEdit> edits =
                provider.generate(candidate(), targetFor(source(), "maybeId"), source());

        assertNotNull(client.context);
        assertEquals("String", client.context.getExpectedType());
        assertEquals(1, edits.size());
        assertEquals("fallbackId", edits.get(0).getReplacementSource());
        assertEquals(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK, edits.get(0).getRepairKind());
        assertEquals(InferenceRepairEditOrigin.AI, edits.get(0).getOrigin());
    }

    @Test
    public void stripsSingleFencedCodeBlock() {
        AiRepairEditProvider provider =
                new AiRepairEditProvider(new CapturingClient("```java\nfallbackId\n```"));

        List<InferenceRepairEdit> edits =
                provider.generate(candidate(), targetFor(source(), "maybeId"), source());

        assertEquals(1, edits.size());
        assertEquals("fallbackId", edits.get(0).getReplacementSource());
    }

    @Test
    public void preservesCandidateRepairKind() {
        AiRepairEditProvider provider = new AiRepairEditProvider(new CapturingClient("replacement"));

        List<InferenceRepairEdit> edits =
                provider.generate(
                        candidate(InferenceRepairKind.INSERT_NULL_GUARD),
                        targetFor(source(), "maybeId"),
                        source());

        assertEquals(1, edits.size());
        assertEquals(InferenceRepairKind.INSERT_NULL_GUARD, edits.get(0).getRepairKind());
    }

    @Test
    public void capsAcceptedAiRepairs() {
        AiRepairEditProvider provider =
                new AiRepairEditProvider(
                        new CapturingClient("first", "first", "second", "third"),
                        2);

        List<InferenceRepairEdit> edits =
                provider.generate(candidate(), targetFor(source(), "maybeId"), source());

        assertEquals(2, edits.size());
        assertEquals("first", edits.get(0).getReplacementSource());
        assertEquals("second", edits.get(1).getReplacementSource());
    }

    @Test
    public void rejectsNonPositiveMaxEdits() {
        try {
            new AiRepairEditProvider(new CapturingClient("fallbackId"), 0);
        } catch (IllegalArgumentException expected) {
            return;
        }

        fail("Expected maxEdits validation to reject zero.");
    }

    @Test
    public void filtersOversizedAiRepairs() {
        AiRepairEditProvider provider =
                new AiRepairEditProvider(
                        new CapturingClient("replacementThatExceedsTheLimit", "fallbackId"),
                        5,
                        "fallbackId".length());

        List<InferenceRepairEdit> edits =
                provider.generate(candidate(), targetFor(source(), "maybeId"), source());

        assertEquals(1, edits.size());
        assertEquals("fallbackId", edits.get(0).getReplacementSource());
    }

    @Test
    public void rejectsNonPositiveMaxReplacementLength() {
        try {
            new AiRepairEditProvider(new CapturingClient("fallbackId"), 5, 0);
        } catch (IllegalArgumentException expected) {
            return;
        }

        fail("Expected maxReplacementLength validation to reject zero.");
    }

    @Test
    public void treatsAiClientFailureAsNoEdits() {
        AiRepairEditProvider provider = new AiRepairEditProvider(new ThrowingClient());

        List<InferenceRepairEdit> edits =
                provider.generate(candidate(), targetFor(source(), "maybeId"), source());

        assertEquals(0, edits.size());
    }

    @Test
    public void passesProjectUsageExamplesToAiClient() throws IOException {
        File directory = new File("build/ai-repair-provider-context-test");
        File targetFile = write(directory, "Test.java", source());
        File safeUsageFile =
                write(
                        directory,
                        "SafeUsage.java",
                        "class SafeUsage {\n"
                                + "  void recordId(String id) {}\n"
                                + "  void setId(String maybeId) {\n"
                                + "    if (maybeId == null) {\n"
                                + "      return;\n"
                                + "    }\n"
                                + "    recordId(maybeId);\n"
                                + "  }\n"
                                + "}\n");
        CapturingClient client = new CapturingClient("fallbackId");
        AiRepairEditProvider provider =
                new AiRepairEditProvider(client, Arrays.asList(targetFile, safeUsageFile));

        provider.generate(candidate(), targetFor(targetFile, source(), "maybeId"), source());

        assertNotNull(client.context);
        assertTrue(
                containsUsageExample(
                        client.context.getUsageExamples(),
                        RepairUsageExample.Kind.SAFE,
                        "if (maybeId == null)"));
    }

    private static String source() {
        return "class Test {\n"
                + "  void recordId(String id) {}\n"
                + "  void setId(String maybeId) {\n"
                + "    String fallbackId = \"unknown\";\n"
                + "    recordId(maybeId);\n"
                + "  }\n"
                + "}\n";
    }

    private static InferenceRepairCandidate candidate() {
        return candidate(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK);
    }

    private static InferenceRepairCandidate candidate(InferenceRepairKind repairKind) {
        InferenceConstraintContext context =
                new InferenceConstraintContext(
                        "EqualityConstraint",
                        "slot#1 == @nninf.qual.Nullable",
                        "AST_PATH",
                        "AstPathLocation( Test.setId(Ljava/lang/String;)V.null:"
                                + "Test:setId(Ljava/lang/String;)V::Method.body,"
                                + " Block.statement 1, ExpressionStatement.expression )",
                        Collections.singletonList(slot()));
        return new InferenceRepairCandidate(
                context,
                slot(),
                repairKind,
                "@Nullable",
                "repair source expression causing @Nullable conflict");
    }

    private static InferenceSlotContext slot() {
        return new InferenceSlotContext(
                1,
                "REFINEMENT_VARIABLE",
                false,
                "AST_PATH",
                "AstPathLocation( Test.setId(Ljava/lang/String;)V.null:"
                        + "Test:setId(Ljava/lang/String;)V::Method.body,"
                        + " Block.statement 1, ExpressionStatement.expression )",
                "slot#1");
    }

    private static InferenceRepairTarget targetFor(String source, String targetText) {
        return targetFor(new File("Test.java"), source, targetText);
    }

    private static InferenceRepairTarget targetFor(File file, String source, String targetText) {
        int start = source.lastIndexOf(targetText);
        return new InferenceRepairTarget(
                file,
                "IDENTIFIER",
                start,
                start + targetText.length(),
                1,
                1,
                targetText);
    }

    private static File write(File directory, String name, String source) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }
        File file = new File(directory, name);
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static boolean containsUsageExample(
            List<RepairUsageExample> examples, RepairUsageExample.Kind kind, String sourceText) {
        for (RepairUsageExample example : examples) {
            if (example.getKind() == kind && example.getSource().contains(sourceText)) {
                return true;
            }
        }
        return false;
    }

    private static final class CapturingClient implements AiRepairClient {
        private final List<String> replacements;
        private RepairPromptContext context;

        private CapturingClient(String... replacements) {
            this.replacements = Arrays.asList(replacements);
        }

        @Override
        public List<String> proposeReplacementSources(RepairPromptContext context) {
            this.context = context;
            return replacements;
        }
    }

    private static final class ThrowingClient implements AiRepairClient {
        @Override
        public List<String> proposeReplacementSources(RepairPromptContext context) {
            throw new IllegalStateException("AI backend unavailable");
        }
    }
}
