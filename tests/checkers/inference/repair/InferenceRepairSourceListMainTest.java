package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class InferenceRepairSourceListMainTest {
    @Test
    public void writesFilteredRelativeSourceList() throws IOException {
        File root = new File("build/inference-repair-source-list-main/project");
        File outputFile = new File("build/inference-repair-source-list-main/sources.txt");
        write(new File(root, "src/main/java/Plain.java"), "class Plain {}\n");
        write(new File(root, "src/main/java/UsesNull.java"), "class UsesNull { Object value = null; }\n");
        write(new File(root, "src/main/java/qual/Nullable.java"), "public @interface Nullable {}\n");

        InferenceRepairSourceListMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--relative-to",
                    root.getPath(),
                    "--source-filter",
                    "NULLNESS_RELEVANT",
                    "--source-root",
                    new File(root, "src/main/java").getPath()
                });

        List<String> lines = InferenceTestUtilities.getLines(outputFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).endsWith("src/main/java/UsesNull.java"));
    }

    private static void write(File file, String source) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }
}
