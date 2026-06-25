package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RepairPromptContextTest {
    @Test
    public void capturesTargetConstraintAndExpectedType() {
        String source =
                "class Test {\n"
                        + "  void recordId(String id) {}\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";
        InferenceRepairTarget target = targetFor(source, "maybeId");

        RepairPromptContext context =
                RepairPromptContext.create(candidate(), target, source);

        assertEquals("String", context.getExpectedType());
        assertEquals("maybeId", context.getOriginalText());
        assertEquals(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK, context.getRepairKind());
        assertEquals("IDENTIFIER", context.getTargetTreeKind());
        assertTrue(context.getConstraintSummary().contains("slot#1"));
        assertTrue(context.toPromptText().contains("Allowed replacement span offsets"));
        assertTrue(context.toPromptText().contains("Return candidate replacement source text"));
    }

    @Test
    public void capturesProjectLocalUsageExamples() {
        String source =
                "class Test {\n"
                        + "  void recordId(String id) {}\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";
        File targetFile = new File("Test.java");
        InferenceRepairTarget target = targetFor(targetFile, source, "maybeId");

        RepairPromptContext context =
                RepairPromptContext.create(
                        candidate(), target, source, Collections.singletonList(targetFile));

        assertTrue(!context.getUsageExamples().isEmpty());
        assertTrue(context.toPromptText().contains("Project-local usage examples"));
        assertTrue(context.toPromptText().contains("recordId(maybeId)"));
    }

    @Test
    public void aiProviderReceivesStructuredPromptContext() {
        CapturingAiProvider provider = new CapturingAiProvider();
        String source =
                "class Test {\n"
                        + "  void recordId(String id) {}\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";

        List<InferenceRepairEdit> edits = provider.generate(candidate(), targetFor(source, "maybeId"), source);

        assertEquals(1, edits.size());
        assertEquals("String", provider.context.getExpectedType());
        assertEquals("maybeId", provider.context.getOriginalText());
    }

    private static InferenceRepairCandidate candidate() {
        InferenceConstraintContext context =
                new InferenceConstraintContext(
                        "EqualityConstraint",
                        "slot#1 == @nninf.qual.Nullable",
                        "AST_PATH",
                        "AstPathLocation( Test.setId(Ljava/lang/String;)V.null:"
                                + "Test:setId(Ljava/lang/String;)V::Method.body,"
                                + " Block.statement 0, ExpressionStatement.expression )",
                        Collections.singletonList(slot()));
        return new InferenceRepairCandidate(
                context,
                slot(),
                InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
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
                        + " Block.statement 0, ExpressionStatement.expression )",
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

    private static final class CapturingAiProvider implements AiInferenceRepairEditProvider {
        private RepairPromptContext context;

        @Override
        public List<InferenceRepairEdit> generate(RepairPromptContext context) {
            this.context = context;
            return Collections.singletonList(
                    new InferenceRepairEdit(
                            InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                            "\"\"",
                            "ai proposed string fallback",
                            "ai_string_fallback"));
        }
    }
}
