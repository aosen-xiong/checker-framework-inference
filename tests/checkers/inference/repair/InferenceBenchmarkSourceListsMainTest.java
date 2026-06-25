package checkers.inference.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class InferenceBenchmarkSourceListsMainTest {
    @Test
    public void writesOneRelativeSourceListPerBenchmarkProject() throws IOException {
        File benchmarkRoot = new File("build/inference-benchmark-source-lists/benchmarks");
        File outputDirectory = new File("build/inference-benchmark-source-lists/lists");
        write(new File(benchmarkRoot, "retrofit/src/main/java/UsesNull.java"), "class UsesNull { Object x = null; }\n");
        write(new File(benchmarkRoot, "retrofit/src/main/java/Plain.java"), "class Plain {}\n");
        write(new File(benchmarkRoot, "retrofit/src/main/java/qual/Nullable.java"), "public @interface Nullable {}\n");
        write(new File(benchmarkRoot, "conductor/src/main/java/UsesOptional.java"), "import java.util.Optional; class UsesOptional {}\n");
        write(new File(benchmarkRoot, ".scratch/Ignored.java"), "class Ignored { Object x = null; }\n");

        InferenceBenchmarkSourceListsMain.main(
                new String[] {
                    "--benchmark-root",
                    benchmarkRoot.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--source-filter",
                    "NULLNESS_RELEVANT"
                });

        File retrofitList = new File(outputDirectory, "retrofit.txt");
        File conductorList = new File(outputDirectory, "conductor.txt");
        assertTrue(retrofitList.isFile());
        assertTrue(conductorList.isFile());
        assertFalse(new File(outputDirectory, ".scratch.txt").exists());

        String retrofit = String.join("\n", InferenceTestUtilities.getLines(retrofitList));
        assertTrue(retrofit.contains("src/main/java/UsesNull.java"));
        assertFalse(retrofit.contains("Plain.java"));
        assertFalse(retrofit.contains("Nullable.java"));
    }

    @Test
    public void filtersBenchmarkProjectsByIncludeAndExcludeLists() throws IOException {
        File benchmarkRoot = new File("build/inference-benchmark-source-lists-filtered/benchmarks");
        File outputDirectory = new File("build/inference-benchmark-source-lists-filtered/lists");
        write(new File(benchmarkRoot, "retrofit/src/A.java"), "class A {}\n");
        write(new File(benchmarkRoot, "conductor/src/B.java"), "class B {}\n");
        write(new File(benchmarkRoot, "scratch/src/C.java"), "class C {}\n");

        InferenceBenchmarkSourceListsMain.main(
                new String[] {
                    "--benchmark-root",
                    benchmarkRoot.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--include-projects",
                    "retrofit,conductor,scratch",
                    "--exclude-projects",
                    "scratch,conductor"
                });

        assertTrue(new File(outputDirectory, "retrofit.txt").isFile());
        assertFalse(new File(outputDirectory, "conductor.txt").exists());
        assertFalse(new File(outputDirectory, "scratch.txt").exists());
    }

    private static void write(File file, String source) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }
}
