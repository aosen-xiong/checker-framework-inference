package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.checkerframework.framework.test.TestUtilities;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RepairDryRunReportTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void reportsNninfTestdataRepairBaseline() {
        List<File> sourceFiles = new ArrayList<>();
        sourceFiles.addAll(TestUtilities.findRelativeNestedJavaFiles("testdata", "nninf"));

        List<RepairDryRunResult> results =
                new SimpleNninfRepairDryRunner(
                                NNINF_CHECKER,
                                NNINF_OPTIONS,
                                new File("build/repair-dry-run/nninf"))
                        .run(sourceFiles);

        printReport(results);

        assertEquals(4, results.size());
        assertEquals(2, totalDiagnostics(results));
        assertEquals(2, totalSupportedDiagnostics(results));
        assertEquals(0, totalUnsupportedDiagnostics(results));
        assertEquals(2, totalConstraints(results));
        assertEquals(2, totalCoreConstraints(results));
        assertEquals(2, totalCandidates(results));
        assertTrue(findResult(results, "FixableError1.java").validated());
    }

    private static void printReport(List<RepairDryRunResult> results) {
        System.out.println("=== nninf repair dry-run report ===");
        System.out.println("files: " + results.size());
        System.out.println("diagnostics: " + totalDiagnostics(results));
        System.out.println("supported diagnostics: " + totalSupportedDiagnostics(results));
        System.out.println("unsupported diagnostics: " + totalUnsupportedDiagnostics(results));
        System.out.println("constraints: " + totalConstraints(results));
        System.out.println("unsat-core constraints: " + totalCoreConstraints(results));
        System.out.println("candidates: " + totalCandidates(results));
        for (RepairDryRunResult result : results) {
            System.out.println(fileSummary(result));
            for (RepairConstraint constraint : result.getConstraints()) {
                System.out.println("  constraint: " + constraint);
            }
            for (RepairCandidate candidate : result.getCandidates()) {
                System.out.println(
                        "  candidate: "
                                + candidate.getDescription()
                                + " at "
                                + candidate.getTargetSlot());
            }
            if (result.getValidationResult() != null) {
                System.out.println(
                        "  validation diagnostics: "
                                + result.getValidationResult()
                                        .getCheckerResult()
                                        .getActualDiagnostics()
                                        .size());
            }
        }
    }

    private static String fileSummary(RepairDryRunResult result) {
        return result.getSourceFile().getName()
                + ": diagnostics="
                + result.getDiagnostics().size()
                + ", supported="
                + result.getSupportedDiagnostics().size()
                + ", unsupported="
                + result.getUnsupportedDiagnostics().size()
                + ", core="
                + result.getCoreResult().getUnsatCore().size()
                + ", candidates="
                + result.getCandidates().size()
                + ", validated="
                + result.validated();
    }

    private static RepairDryRunResult findResult(List<RepairDryRunResult> results, String name) {
        for (RepairDryRunResult result : results) {
            if (result.getSourceFile().getName().equals(name)) {
                return result;
            }
        }
        throw new AssertionError("Missing result for " + name);
    }

    private static int totalDiagnostics(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getDiagnostics().size();
        }
        return total;
    }

    private static int totalSupportedDiagnostics(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getSupportedDiagnostics().size();
        }
        return total;
    }

    private static int totalUnsupportedDiagnostics(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getUnsupportedDiagnostics().size();
        }
        return total;
    }

    private static int totalConstraints(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getConstraints().size();
        }
        return total;
    }

    private static int totalCoreConstraints(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getCoreResult().getUnsatCore().size();
        }
        return total;
    }

    private static int totalCandidates(List<RepairDryRunResult> results) {
        int total = 0;
        for (RepairDryRunResult result : results) {
            total += result.getCandidates().size();
        }
        return total;
    }
}
