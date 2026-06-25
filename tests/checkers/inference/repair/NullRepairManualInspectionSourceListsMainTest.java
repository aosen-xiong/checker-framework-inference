package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class NullRepairManualInspectionSourceListsMainTest {
    @Test
    public void parsesCasesAndStripsBenchmarkPrefixAndLineNumber() throws IOException {
        File input = new File("build/nullrepair-manual-parser/manual.tsv");
        write(input, tsvHeader() + eurekaRow("1", "1", "3", "1") + glideRow("16", "2", "3", "3"));

        List<NullRepairManualInspectionCase> cases = NullRepairManualInspectionParser.parse(input);

        assertEquals(2, cases.size());
        NullRepairManualInspectionCase eureka = cases.get(0);
        assertEquals("eureka", eureka.getBenchmark());
        assertEquals(44, eureka.lineNumber());
        assertEquals(
                "eureka-core/src/main/java/com/netflix/eureka/EurekaServerContextHolder.java",
                eureka.sourcePathRelativeToBenchmarkRoot());
        assertEquals(1, eureka.bestScore());
    }

    @Test
    public void writesFilteredSourceListsFromManualInspectionRows() throws IOException {
        File input = new File("build/nullrepair-manual-source-lists/manual.tsv");
        File outputDirectory = new File("build/nullrepair-manual-source-lists/lists");
        write(
                input,
                tsvHeader()
                        + eurekaRow("1", "1", "3", "1")
                        + eurekaRow("2", "3", "3", "3")
                        + glideRow("16", "2", "3", "3"));

        NullRepairManualInspectionSourceListsMain.main(
                new String[] {
                    "--input",
                    input.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--include-projects",
                    "eureka,glide",
                    "--max-best-score",
                    "2"
                });

        File eurekaList = new File(outputDirectory, "eureka.txt");
        File glideList = new File(outputDirectory, "glide.txt");
        assertTrue(eurekaList.isFile());
        assertTrue(glideList.isFile());

        String eureka = String.join("\n", InferenceTestUtilities.getLines(eurekaList));
        assertTrue(eureka.contains("EurekaServerContextHolder.java"));
        assertFalse(eureka.contains("RateLimitingFilter.java"));
        assertFalse(eureka.contains("eureka/eureka-core"));
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
