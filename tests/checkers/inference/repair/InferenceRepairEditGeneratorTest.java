package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;

public class InferenceRepairEditGeneratorTest {
    private final InferenceRepairEditGenerator generator = new InferenceRepairEditGenerator();

    @Test
    public void prefersInScopeStringLocalBeforeLiteralFallback() {
        String source =
                "class Test {\n"
                        + "  void setId(String maybeId) {\n"
                        + "    String fallbackId = \"unknown\";\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";
        InferenceRepairTarget target = targetFor(source, "maybeId");

        List<InferenceRepairEdit> edits =
                generator.generate(candidate(), target, source);

        assertEquals("fallbackId", edits.get(0).getReplacementSource());
        assertEquals("\"\"", edits.get(1).getReplacementSource());
    }

    @Test
    public void fallsBackToStringLiteralWhenNoLocalExists() {
        String source =
                "class Test {\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";
        InferenceRepairTarget target = targetFor(source, "maybeId");

        List<InferenceRepairEdit> edits =
                generator.generate(candidate(), target, source);

        assertEquals(1, edits.size());
        assertEquals("\"\"", edits.get(0).getReplacementSource());
    }

    @Test
    public void doesNotUseNullableStringLocalAsFallback() {
        String source =
                "class Test {\n"
                        + "  void setId(String maybeId) {\n"
                        + "    @Nullable String nullableId = maybeId;\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";
        InferenceRepairTarget target = targetFor(source, "maybeId");

        List<InferenceRepairEdit> edits =
                generator.generate(candidate(), target, source);

        assertEquals(1, edits.size());
        assertEquals("\"\"", edits.get(0).getReplacementSource());
    }

    private static InferenceRepairCandidate candidate() {
        return new InferenceRepairCandidate(
                null,
                new InferenceSlotContext(
                        1,
                        "REFINEMENT_VARIABLE",
                        false,
                        "AST_PATH",
                        "AstPathLocation( Test.setId(Ljava/lang/String;)V.null:"
                                + "Test:setId(Ljava/lang/String;)V::Method.body,"
                                + " Block.statement 1, ExpressionStatement.expression )",
                        "slot#1"),
                InferenceRepairKind.INSERT_NULL_GUARD,
                "@Nullable",
                "repair source expression causing @Nullable conflict");
    }

    private static InferenceRepairTarget targetFor(String source, String targetText) {
        int start = source.lastIndexOf(targetText);
        return new InferenceRepairTarget(
                new File("Test.java"),
                "IDENTIFIER",
                start,
                start + targetText.length(),
                1,
                1,
                targetText);
    }
}
