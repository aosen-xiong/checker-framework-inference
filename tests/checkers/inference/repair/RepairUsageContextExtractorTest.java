package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RepairUsageContextExtractorTest {
    private final RepairUsageContextExtractor extractor = new RepairUsageContextExtractor();

    @Test
    public void retrievesSafeAndUnsafeUsagesForIdentifierTarget() throws IOException {
        File directory = new File("build/repair-usage-context-test");
        File targetFile = write(
                directory,
                "Target.java",
                "class Target {\n"
                        + "  void recordId(String id) {}\n"
                        + "  void setId(String maybeId) {\n"
                        + "    recordId(maybeId);\n"
                        + "  }\n"
                        + "}\n");
        File safeFile = write(
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
        String targetSource =
                new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);

        List<RepairUsageExample> examples =
                extractor.extract(
                        candidate(),
                        targetFor(targetFile, targetSource, "maybeId"),
                        targetSource,
                        Arrays.asList(targetFile, safeFile));

        assertTrue(containsExample(examples, RepairUsageExample.Kind.SAFE, "if (maybeId == null)"));
        assertTrue(containsExample(examples, RepairUsageExample.Kind.UNSAFE, "recordId(maybeId)"));
    }

    @Test
    public void retrievesSafeAndUnsafeUsagesForMethodInvocationTarget() throws IOException {
        File directory = new File("build/repair-usage-context-test");
        File targetFile =
                write(
                        directory,
                        "MapViewTarget.java",
                        "class MapViewTarget {\n"
                                + "  void move(Point p) {}\n"
                                + "  void update(User user) {\n"
                                + "    move(user.getMapView().getCenter());\n"
                                + "  }\n"
                                + "}\n");
        File safeFile =
                write(
                        directory,
                        "MapViewSafe.java",
                        "class MapViewSafe {\n"
                                + "  Point center(User user) {\n"
                                + "    if (user.getMapView() == null) {\n"
                                + "      return Point.ORIGIN;\n"
                                + "    }\n"
                                + "    return user.getMapView().getCenter();\n"
                                + "  }\n"
                                + "}\n");
        String targetSource =
                new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        InferenceRepairTarget target =
                targetFor(targetFile, targetSource, "user.getMapView()", "METHOD_INVOCATION");

        List<RepairUsageExample> examples =
                extractor.extract(
                        candidate(), target, targetSource, Arrays.asList(targetFile, safeFile));

        assertTrue(
                containsExample(
                        examples, RepairUsageExample.Kind.SAFE, "user.getMapView() == null"));
        assertTrue(
                containsExample(
                        examples, RepairUsageExample.Kind.UNSAFE,
                        "move(user.getMapView().getCenter())"));
    }

    private static File write(File directory, String name, String source) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }
        File file = new File(directory, name);
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static boolean containsExample(
            List<RepairUsageExample> examples, RepairUsageExample.Kind kind, String sourceText) {
        for (RepairUsageExample example : examples) {
            if (example.getKind() == kind && example.getSource().contains(sourceText)) {
                return true;
            }
        }
        return false;
    }

    private static InferenceRepairTarget targetFor(File file, String source, String targetText) {
        return targetFor(file, source, targetText, "IDENTIFIER");
    }

    private static InferenceRepairTarget targetFor(
            File file, String source, String targetText, String treeKind) {
        int start = source.lastIndexOf(targetText);
        return new InferenceRepairTarget(
                file,
                treeKind,
                start,
                start + targetText.length(),
                1,
                1,
                targetText);
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
}
