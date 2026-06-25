package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class NullRepairManualInspectionMaterializerMainTest {
    @Test
    public void copiesSelectedManualInspectionSourcesFromLocalBenchmarkRoot() throws IOException {
        File root = new File("build/nullrepair-materializer/local/benchmarks");
        File input = new File("build/nullrepair-materializer/local/manual.tsv");
        File outputRoot = new File("build/nullrepair-materializer/local/out-benchmarks");
        File sourceListOutput = new File("build/nullrepair-materializer/local/source-lists");
        File caseManifest = new File("build/nullrepair-materializer/local/case-manifest.csv");
        String eurekaSource = "class EurekaServerContextHolder {}\n";
        String filteredSource = "class RateLimitingFilter {}\n";
        write(
                new File(
                        root,
                        "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContextHolder.java"),
                eurekaSource);
        write(
                new File(
                        root,
                        "eureka/eureka-core/src/main/java/com/netflix/eureka/RateLimitingFilter.java"),
                filteredSource);
        write(input, tsvHeader() + eurekaRow("1", "1", "3", "1") + eurekaRow("2", "3", "3", "3"));

        NullRepairManualInspectionMaterializerMain.main(
                new String[] {
                    "--input",
                    input.getPath(),
                    "--benchmark-root",
                    root.getPath(),
                    "--out-root",
                    outputRoot.getPath(),
                    "--source-list-out-dir",
                    sourceListOutput.getPath(),
                    "--case-manifest-out",
                    caseManifest.getPath(),
                    "--include-projects",
                    "eureka",
                    "--max-best-score",
                    "1"
                });

        File materialized =
                new File(
                        outputRoot,
                        "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContextHolder.java");
        assertTrue(materialized.isFile());
        assertEquals(eurekaSource, new String(Files.readAllBytes(materialized.toPath()), StandardCharsets.UTF_8));

        File sourceList = new File(sourceListOutput, "eureka.txt");
        assertTrue(sourceList.isFile());
        String sourceListText = String.join("\n", InferenceTestUtilities.getLines(sourceList));
        assertTrue(sourceListText.contains("EurekaServerContextHolder.java"));
        assertTrue(!sourceListText.contains("RateLimitingFilter.java"));

        String manifest = String.join("\n", InferenceTestUtilities.getLines(caseManifest));
        assertTrue(manifest.contains("\"eureka\",\"1\",\"RETURN_NULLABLE\""));
        assertTrue(manifest.contains("\"return holder;\""));
        assertTrue(!manifest.contains("\"eureka\",\"2\""));
    }

    @Test
    public void downloadsSelectedManualInspectionSourcesFromUrlRoot() throws IOException {
        File root = new File("build/nullrepair-materializer/url/remote-benchmarks");
        File input = new File("build/nullrepair-materializer/url/manual.tsv");
        File outputRoot = new File("build/nullrepair-materializer/url/out-benchmarks");
        File sourceListOutput = new File("build/nullrepair-materializer/url/source-lists");
        String source = "class DecodeHelper {}\n";
        write(
                new File(
                        root,
                        "glide/library/src/main/java/com/bumptech/glide/load/engine/DecodeHelper.java"),
                source);
        write(input, tsvHeader() + glideRow("16", "1", "3", "3"));

        NullRepairManualInspectionMaterializerMain.main(
                new String[] {
                    "--input",
                    input.getPath(),
                    "--artifact-benchmarks-url",
                    root.toURI().toURL().toString(),
                    "--out-root",
                    outputRoot.getPath(),
                    "--source-list-out-dir",
                    sourceListOutput.getPath(),
                    "--include-projects",
                    "glide"
                });

        File materialized =
                new File(
                        outputRoot,
                        "glide/library/src/main/java/com/bumptech/glide/load/engine/DecodeHelper.java");
        assertTrue(materialized.isFile());
        assertEquals(source, new String(Files.readAllBytes(materialized.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void materializesLikelySourceClosureWhenRequested() throws IOException {
        File root = new File("build/nullrepair-materializer/closure/benchmarks");
        File input = new File("build/nullrepair-materializer/closure/manual.tsv");
        File outputRoot = new File("build/nullrepair-materializer/closure/out-benchmarks");
        File sourceListOutput = new File("build/nullrepair-materializer/closure/source-lists");
        File targetSourceListOutput = new File("build/nullrepair-materializer/closure/target-source-lists");
        write(
                new File(
                        root,
                        "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContextHolder.java"),
                "package com.netflix.eureka;\n"
                        + "import com.netflix.eureka.support.Helper;\n"
                        + "class EurekaServerContextHolder { EurekaServerContext c; Helper h; }\n");
        write(
                new File(root, "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContext.java"),
                "package com.netflix.eureka;\nclass EurekaServerContext {}\n");
        write(
                new File(root, "eureka/eureka-core/src/main/java/com/netflix/eureka/support/Helper.java"),
                "package com.netflix.eureka.support;\npublic class Helper {}\n");
        write(input, tsvHeader() + eurekaRow("1", "1", "3", "1"));

        NullRepairManualInspectionMaterializerMain.main(
                new String[] {
                    "--input",
                    input.getPath(),
                    "--benchmark-root",
                    root.getPath(),
                    "--out-root",
                    outputRoot.getPath(),
                    "--source-list-out-dir",
                    sourceListOutput.getPath(),
                    "--target-source-list-out-dir",
                    targetSourceListOutput.getPath(),
                    "--include-projects",
                    "eureka",
                    "--max-best-score",
                    "1",
                    "--closure-depth",
                    "1"
                });

        assertTrue(
                new File(
                                outputRoot,
                                "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContext.java")
                        .isFile());
        assertTrue(
                new File(
                                outputRoot,
                                "eureka/eureka-core/src/main/java/com/netflix/eureka/support/Helper.java")
                        .isFile());
        String sourceList =
                String.join("\n", InferenceTestUtilities.getLines(new File(sourceListOutput, "eureka.txt")));
        assertTrue(sourceList.contains("EurekaServerContextHolder.java"));
        assertTrue(sourceList.contains("EurekaServerContext.java"));
        assertTrue(sourceList.contains("support/Helper.java"));

        String targetSourceList =
                String.join(
                        "\n", InferenceTestUtilities.getLines(new File(targetSourceListOutput, "eureka.txt")));
        assertTrue(targetSourceList.contains("EurekaServerContextHolder.java"));
        assertTrue(!targetSourceList.contains("EurekaServerContext.java"));
        assertTrue(!targetSourceList.contains("support/Helper.java"));
    }

    private static String tsvHeader() {
        return "Benchmark\tID\tTool A\tTool B\tTool C\tPatch A\tPatch B\tPatch C\tType\tMessage\tPath\tExpression\t"
                + "SCORE (Patch A) Consolidated\tSCORE (Patch B) Consolidated\tSCORE (Patch C) Consolidated\n";
    }

    private static String eurekaRow(String id, String scoreA, String scoreB, String scoreC) {
        String path;
        if ("1".equals(id)) {
            path =
                    "eureka/eureka-core/src/main/java/com/netflix/eureka/EurekaServerContextHolder.java:44";
        } else {
            path = "eureka/eureka-core/src/main/java/com/netflix/eureka/RateLimitingFilter.java:113";
        }
        return "eureka\t"
                + id
                + "\thash_a\thash_b\thash_c\tGithub\tGithub\tGithub\tRETURN_NULLABLE\tmessage\t"
                + path
                + "\treturn holder;\t"
                + scoreA
                + "\t"
                + scoreB
                + "\t"
                + scoreC
                + "\n";
    }

    private static String glideRow(String id, String scoreA, String scoreB, String scoreC) {
        return "glide\t"
                + id
                + "\thash_a\thash_b\thash_c\tGithub\tGithub\tGithub\tASSIGN_FIELD_NULLABLE\tmessage\t"
                + "glide/library/src/main/java/com/bumptech/glide/load/engine/DecodeHelper.java:84\t"
                + "resourceClass = null;\t"
                + scoreA
                + "\t"
                + scoreB
                + "\t"
                + scoreC
                + "\n";
    }

    private static void write(File file, String contents) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
