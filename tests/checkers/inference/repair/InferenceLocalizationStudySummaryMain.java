package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Summarizes localization-study CSV reports for quick benchmark triage. */
public final class InferenceLocalizationStudySummaryMain {
    private InferenceLocalizationStudySummaryMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<Map<String, String>> rows = readCsv(arguments.inputFile);
        Summary summary = Summary.from(rows);
        writeReport(arguments.outputFile, summary.toMarkdown(arguments.inputFile));
        System.out.println(summary.oneLine(arguments.inputFile));
    }

    private static List<Map<String, String>> readCsv(File inputFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read localization CSV: " + inputFile, e);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Localization CSV is empty: " + inputFile);
        }
        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < headers.size(); index++) {
                row.put(headers.get(index), index < values.size() ? values.get(index) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);
            if (quoted) {
                if (c == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static void writeReport(File outputFile, String report) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create summary directory: " + parent);
        }
        try {
            Files.write(outputFile.toPath(), report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write localization summary: " + outputFile, e);
        }
    }

    private static final class Summary {
        private final int total;
        private final int runErrors;
        private final int sat;
        private final int unsat;
        private final int timeout;
        private final int solverBacked;
        private final int unsatWithCandidates;
        private final int unsatWithSourceRealizableCandidate;
        private final int sourceRealizable;
        private final int unsatSourceRealizable;
        private final int repairPlanMaterialized;
        private final int repairPlanAttempted;
        private final int repairPlanInferenceSolved;
        private final int repairPlanVerified;
        private final int repairFollowUpAttempted;
        private final int repairFollowUpVerified;
        private final int boundedSearch;
        private final int truncatedUniverse;
        private final Map<String, Integer> runErrorKinds;
        private final List<String> missingDependencyExamples;

        private Summary(
                int total,
                int runErrors,
                int sat,
                int unsat,
                int timeout,
                int solverBacked,
                int unsatWithCandidates,
                int unsatWithSourceRealizableCandidate,
                int sourceRealizable,
                int unsatSourceRealizable,
                int repairPlanMaterialized,
                int repairPlanAttempted,
                int repairPlanInferenceSolved,
                int repairPlanVerified,
                int repairFollowUpAttempted,
                int repairFollowUpVerified,
                int boundedSearch,
                int truncatedUniverse,
                Map<String, Integer> runErrorKinds,
                List<String> missingDependencyExamples) {
            this.total = total;
            this.runErrors = runErrors;
            this.sat = sat;
            this.unsat = unsat;
            this.timeout = timeout;
            this.solverBacked = solverBacked;
            this.unsatWithCandidates = unsatWithCandidates;
            this.unsatWithSourceRealizableCandidate = unsatWithSourceRealizableCandidate;
            this.sourceRealizable = sourceRealizable;
            this.unsatSourceRealizable = unsatSourceRealizable;
            this.repairPlanMaterialized = repairPlanMaterialized;
            this.repairPlanAttempted = repairPlanAttempted;
            this.repairPlanInferenceSolved = repairPlanInferenceSolved;
            this.repairPlanVerified = repairPlanVerified;
            this.repairFollowUpAttempted = repairFollowUpAttempted;
            this.repairFollowUpVerified = repairFollowUpVerified;
            this.boundedSearch = boundedSearch;
            this.truncatedUniverse = truncatedUniverse;
            this.runErrorKinds = runErrorKinds;
            this.missingDependencyExamples = missingDependencyExamples;
        }

        private static Summary from(List<Map<String, String>> rows) {
            int runErrors = 0;
            int sat = 0;
            int unsat = 0;
            int timeout = 0;
            int solverBacked = 0;
            int unsatWithCandidates = 0;
            int unsatWithSourceRealizableCandidate = 0;
            int sourceRealizable = 0;
            int unsatSourceRealizable = 0;
            int repairPlanMaterialized = 0;
            int repairPlanAttempted = 0;
            int repairPlanInferenceSolved = 0;
            int repairPlanVerified = 0;
            int repairFollowUpAttempted = 0;
            int repairFollowUpVerified = 0;
            int boundedSearch = 0;
            int truncatedUniverse = 0;
            Map<String, Integer> runErrorKinds = new LinkedHashMap<>();
            Set<String> missingDependencyExamples = new LinkedHashSet<>();
            for (Map<String, String> row : rows) {
                String runError = row.get("runError");
                boolean hasRunError = runError != null && !runError.isEmpty();
                if (hasRunError) {
                    runErrors++;
                    increment(runErrorKinds, RunErrorClassifier.classify(runError));
                    for (String example :
                            RunErrorClassifier.missingDependencyExamples(runError, 8)) {
                        if (missingDependencyExamples.size() < 12) {
                            missingDependencyExamples.add(example);
                        }
                    }
                    if (runError.startsWith("TIMEOUT")) {
                        timeout++;
                    }
                }
                if ("true".equals(row.get("solverHadSolution"))) {
                    sat++;
                } else if (!hasRunError) {
                    unsat++;
                    if (hasCandidate(row)) {
                        unsatWithCandidates++;
                    }
                    if (hasSourceRealizableCandidate(row)) {
                        unsatWithSourceRealizableCandidate++;
                    }
                    if (hasSourceRealizableUnit(row)) {
                        unsatSourceRealizable++;
                    }
                }
                if (hasSourceRealizableUnit(row)) {
                    sourceRealizable++;
                }
                if ("true".equals(row.get("sourceRepairPlanMaterialized"))) {
                    repairPlanMaterialized++;
                }
                if (repairPlanAttempted(row)) {
                    repairPlanAttempted++;
                }
                if ("true".equals(row.get("sourceRepairPlanInferenceSolved"))) {
                    repairPlanInferenceSolved++;
                }
                if ("true".equals(row.get("sourceRepairPlanVerified"))) {
                    repairPlanVerified++;
                }
                if ("true".equals(row.get("sourceRepairFollowUpAttempted"))) {
                    repairFollowUpAttempted++;
                }
                if ("true".equals(row.get("sourceRepairFollowUpVerified"))) {
                    repairFollowUpVerified++;
                }
                if ("SOLVER_BACKED".equals(row.get("mcsOracleKind"))) {
                    solverBacked++;
                }
                if ("true".equals(row.get("mcsSearchBounded"))) {
                    boundedSearch++;
                }
                if ("true".equals(row.get("mcsUniverseTruncated"))) {
                    truncatedUniverse++;
                }
            }
            return new Summary(
                    rows.size(),
                    runErrors,
                    sat,
                    unsat,
                    timeout,
                    solverBacked,
                    unsatWithCandidates,
                    unsatWithSourceRealizableCandidate,
                    sourceRealizable,
                    unsatSourceRealizable,
                    repairPlanMaterialized,
                    repairPlanAttempted,
                    repairPlanInferenceSolved,
                    repairPlanVerified,
                    repairFollowUpAttempted,
                    repairFollowUpVerified,
                    boundedSearch,
                    truncatedUniverse,
                    runErrorKinds,
                    new ArrayList<>(missingDependencyExamples));
        }

        private static void increment(Map<String, Integer> counts, String key) {
            Integer value = counts.get(key);
            counts.put(key, value == null ? 1 : value + 1);
        }

        private static boolean hasCandidate(Map<String, String> row) {
            String top1Locations = row.get("top1Locations");
            return top1Locations != null && !top1Locations.isEmpty();
        }

        private static boolean hasSourceRealizableCandidate(Map<String, String> row) {
            return "true".equals(row.get("top1HasSourceRealizableRepairUnit"));
        }

        private static boolean hasSourceRealizableUnit(Map<String, String> row) {
            return "true".equals(row.get("hasSourceRealizableRepairUnit"));
        }

        private static boolean repairPlanAttempted(Map<String, String> row) {
            return "true".equals(row.get("sourceRepairPlanMaterialized"))
                    || "true".equals(row.get("sourceRepairPlanInferenceSolved"))
                    || "true".equals(row.get("sourceRepairPlanVerified"))
                    || "true".equals(row.get("sourceRepairFollowUpAttempted"));
        }

        private String oneLine(File inputFile) {
            return "Localization summary for "
                    + inputFile.getPath()
                    + ": total="
                    + total
                    + ", unsat="
                    + unsat
                    + ", sat="
                    + sat
                    + ", runErrors="
                    + runErrors
                    + ", timeouts="
                    + timeout;
        }

        private String toMarkdown(File inputFile) {
            StringBuilder report = new StringBuilder();
            report.append("# Localization Study Summary\n\n");
            report.append("- Input: `").append(inputFile.getPath()).append("`\n");
            report.append("- Total cases: ").append(total).append("\n");
            report.append("- SAT cases: ").append(sat).append("\n");
            report.append("- UNSAT cases: ").append(unsat).append("\n");
            report.append("- Run errors: ").append(runErrors).append("\n");
            report.append("- Timeouts: ").append(timeout).append("\n");
            if (!runErrorKinds.isEmpty()) {
                report.append("- Run error kinds:\n");
                for (Map.Entry<String, Integer> entry : runErrorKinds.entrySet()) {
                    report.append("  - ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                }
            }
            if (!missingDependencyExamples.isEmpty()) {
                report.append("- Missing dependency examples:\n");
                for (String example : missingDependencyExamples) {
                    report.append("  - `").append(example).append("`\n");
                }
            }
            report.append("- Solver-backed rows: ").append(solverBacked).append("\n");
            report.append("- UNSAT rows with top-1 MCS candidates: ")
                    .append(unsatWithCandidates)
                    .append("\n");
            report.append("- UNSAT rows with source-realizable top-1 MCS candidates: ")
                    .append(unsatWithSourceRealizableCandidate)
                    .append("\n");
            report.append("- Rows with source-realizable repair units: ")
                    .append(sourceRealizable)
                    .append("\n");
            report.append("- UNSAT rows with source-realizable repair units: ")
                    .append(unsatSourceRealizable)
                    .append("\n");
            report.append("- Source repair plans materialized: ")
                    .append(repairPlanMaterialized)
                    .append("\n");
            report.append("- Source repair plan validations attempted: ")
                    .append(repairPlanAttempted)
                    .append("\n");
            report.append("- Validation attempts that solved inference: ")
                    .append(repairPlanInferenceSolved)
                    .append("\n");
            report.append("- Validation attempts fully verified: ")
                    .append(repairPlanVerified)
                    .append("\n");
            report.append("- Residual diagnostic repairs attempted: ")
                    .append(repairFollowUpAttempted)
                    .append("\n");
            report.append("- Residual diagnostic repairs verified: ")
                    .append(repairFollowUpVerified)
                    .append("\n");
            report.append("- Bounded MCS searches: ").append(boundedSearch).append("\n");
            report.append("- Truncated MCS universes: ").append(truncatedUniverse).append("\n\n");
            report.append("## Triage\n\n");
            report.append("- UNSAT yield: ").append(percent(unsat, total)).append("\n");
            report.append("- Timeout rate: ").append(percent(timeout, total)).append("\n");
            report.append("- Candidate yield among UNSAT: ")
                    .append(percent(unsatWithCandidates, unsat))
                    .append("\n");
            report.append("- Source-realizable top-1 candidate yield among UNSAT: ")
                    .append(percent(unsatWithSourceRealizableCandidate, unsat))
                    .append("\n");
            report.append("- Source-realizable yield among UNSAT: ")
                    .append(percent(unsatSourceRealizable, unsat))
                    .append("\n");
            report.append("- Materialization yield among UNSAT: ")
                    .append(percent(repairPlanMaterialized, unsat))
                    .append("\n");
            report.append("- Repair validation attempt yield among UNSAT: ")
                    .append(percent(repairPlanAttempted, unsat))
                    .append("\n");
            report.append("- Inference-solved yield among validation attempts: ")
                    .append(percent(repairPlanInferenceSolved, repairPlanAttempted))
                    .append("\n");
            report.append("- Full verification yield among validation attempts: ")
                    .append(percent(repairPlanVerified, repairPlanAttempted))
                    .append("\n");
            report.append("- Residual diagnostic repair verification yield: ")
                    .append(percent(repairFollowUpVerified, repairFollowUpAttempted))
                    .append("\n");
            return report.toString();
        }

        private static String percent(int numerator, int denominator) {
            if (denominator == 0) {
                return "n/a";
            }
            return Math.round((1000.0 * numerator) / denominator) / 10.0 + "%";
        }
    }

    private static final class Arguments {
        private final File inputFile;
        private final File outputFile;

        private Arguments(File inputFile, File outputFile) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        private static Arguments parse(String[] args) {
            File inputFile = null;
            File outputFile = new File("build/inference-localization-study/summary.md");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--csv".equals(arg)) {
                    inputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null) {
                throw new IllegalArgumentException("--csv is required.");
            }
            return new Arguments(inputFile, outputFile);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
