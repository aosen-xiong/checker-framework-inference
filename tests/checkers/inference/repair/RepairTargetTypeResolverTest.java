package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class RepairTargetTypeResolverTest {
    @Test
    public void resolvesMethodArgumentExpectedType() {
        String source =
                "class Test {\n"
                        + "  void recordCount(Integer count) {}\n"
                        + "  void setCount(Integer maybeCount) {\n"
                        + "    recordCount(maybeCount);\n"
                        + "  }\n"
                        + "}\n";

        assertEquals(
                "Integer",
                RepairTargetTypeResolver.expectedType(targetFor(source, "maybeCount"), source));
    }

    @Test
    public void resolvesFieldAssignmentExpectedType() {
        String source =
                "class Test {\n"
                        + "  Boolean enabled = false;\n"
                        + "  void setEnabled(Boolean maybeEnabled) {\n"
                        + "    enabled = maybeEnabled;\n"
                        + "  }\n"
                        + "}\n";

        assertEquals(
                "Boolean",
                RepairTargetTypeResolver.expectedType(targetFor(source, "maybeEnabled"), source));
    }

    @Test
    public void stripsNullnessAnnotationsFromExpectedType() {
        String source =
                "class Test {\n"
                        + "  void recordId(@NonNull String id) {}\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n";

        assertEquals(
                "String",
                RepairTargetTypeResolver.expectedType(targetFor(source, "maybeId"), source));
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
