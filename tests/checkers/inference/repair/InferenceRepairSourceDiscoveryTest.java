package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class InferenceRepairSourceDiscoveryTest {
    @Test
    public void discoversJavaSourcesRecursively() throws IOException {
        File root = new File("build/inference-repair-source-discovery");
        write(new File(root, "A.java"), "class A {}\n");
        write(new File(root, "nested/B.java"), "class B {}\n");
        write(new File(root, "nested/readme.txt"), "ignore\n");

        List<File> sources =
                InferenceRepairSourceDiscovery.discoverJavaSources(Collections.singletonList(root));

        assertEquals(2, sources.size());
        assertTrue(sources.get(0).getPath().endsWith("A.java"));
        assertTrue(sources.get(1).getPath().endsWith("B.java"));
    }

    @Test
    public void filtersNullnessRelevantSources() throws IOException {
        File root = new File("build/inference-repair-source-discovery-filtered");
        write(new File(root, "Plain.java"), "class Plain { int value; }\n");
        write(new File(root, "UsesNull.java"), "class UsesNull { Object value = null; }\n");
        write(new File(root, "UsesOptional.java"), "import java.util.Optional; class UsesOptional {}\n");
        write(new File(root, "qual/Nullable.java"), "public @interface Nullable {}\n");

        List<File> sources =
                InferenceRepairSourceDiscovery.discoverJavaSources(
                        Collections.singletonList(root),
                        InferenceRepairSourceDiscovery.SourceFilter.NULLNESS_RELEVANT);

        assertEquals(2, sources.size());
        assertTrue(sources.get(0).getPath().endsWith("UsesNull.java"));
        assertTrue(sources.get(1).getPath().endsWith("UsesOptional.java"));
    }

    @Test
    public void readsSourceListRelativeToProjectRoot() throws IOException {
        File root = new File("build/inference-repair-source-discovery-relative");
        File sourceList = new File(root, "sources.txt");
        write(new File(root, "src/main/java/UsesNull.java"), "class UsesNull { Object value = null; }\n");
        write(sourceList, "# comment\nsrc/main/java/UsesNull.java\n\n");

        List<File> sources = InferenceRepairSourceDiscovery.readSourceList(sourceList, root);

        assertEquals(1, sources.size());
        assertEquals(
                new File(root, "src/main/java/UsesNull.java").getPath(),
                sources.get(0).getPath());
    }

    private static void write(File file, String source) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }
}
