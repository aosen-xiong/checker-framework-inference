package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes a compact dependency-blocker report from localization-study CSV output. */
public final class InferenceLocalizationDependencyReportMain {
    private InferenceLocalizationDependencyReportMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<Map<String, String>> rows = readCsv(arguments.inputFile);
        Report report = Report.from(rows, arguments.exampleLimit);
        writeReport(arguments.outputFile, report.toCsv());
        System.out.println(
                "Dependency report for "
                        + arguments.inputFile.getPath()
                        + ": blockedRows="
                        + report.blockedRows
                        + ", missingDependencyRows="
                        + report.missingDependencyRows);
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
            throw new IllegalStateException("Could not create dependency report directory: " + parent);
        }
        try {
            Files.write(outputFile.toPath(), report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write dependency report: " + outputFile, e);
        }
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static final class Report {
        private final List<ReportRow> rows;
        private final int blockedRows;
        private final int missingDependencyRows;

        private Report(List<ReportRow> rows, int blockedRows, int missingDependencyRows) {
            this.rows = rows;
            this.blockedRows = blockedRows;
            this.missingDependencyRows = missingDependencyRows;
        }

        private static Report from(List<Map<String, String>> rows, int exampleLimit) {
            List<ReportRow> reportRows = new ArrayList<>();
            int blockedRows = 0;
            int missingDependencyRows = 0;
            for (Map<String, String> row : rows) {
                String runError = row.get("runError");
                if (runError == null || runError.isEmpty()) {
                    continue;
                }
                blockedRows++;
                String kind = RunErrorClassifier.classify(runError);
                List<String> examples =
                        RunErrorClassifier.missingDependencyExamples(runError, exampleLimit);
                if (!examples.isEmpty()) {
                    missingDependencyRows++;
                }
                reportRows.add(
                        new ReportRow(
                                row.get("projectName"),
                                row.get("sourceFile"),
                                kind,
                                examples));
            }
            return new Report(reportRows, blockedRows, missingDependencyRows);
        }

        private String toCsv() {
            StringBuilder report = new StringBuilder();
            report.append("projectName,sourceFile,runErrorKind,missingDependencyExamples\n");
            for (ReportRow row : rows) {
                report.append(csv(row.projectName))
                        .append(",")
                        .append(csv(row.sourceFile))
                        .append(",")
                        .append(csv(row.runErrorKind))
                        .append(",")
                        .append(csv(String.join("; ", row.missingDependencyExamples)))
                        .append("\n");
            }
            return report.toString();
        }
    }

    private static final class ReportRow {
        private final String projectName;
        private final String sourceFile;
        private final String runErrorKind;
        private final List<String> missingDependencyExamples;

        private ReportRow(
                String projectName,
                String sourceFile,
                String runErrorKind,
                List<String> missingDependencyExamples) {
            this.projectName = projectName;
            this.sourceFile = sourceFile;
            this.runErrorKind = runErrorKind;
            this.missingDependencyExamples = missingDependencyExamples;
        }
    }

    private static final class Arguments {
        private final File inputFile;
        private final File outputFile;
        private final int exampleLimit;

        private Arguments(File inputFile, File outputFile, int exampleLimit) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.exampleLimit = exampleLimit;
        }

        private static Arguments parse(String[] args) {
            File inputFile = null;
            File outputFile = new File("build/inference-localization-study/dependencies.csv");
            int exampleLimit = 12;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--csv".equals(arg)) {
                    inputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--example-limit".equals(arg)) {
                    exampleLimit = Integer.parseInt(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null) {
                throw new IllegalArgumentException("--csv is required.");
            }
            if (exampleLimit < 0) {
                throw new IllegalArgumentException("--example-limit must be non-negative.");
            }
            return new Arguments(inputFile, outputFile, exampleLimit);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
