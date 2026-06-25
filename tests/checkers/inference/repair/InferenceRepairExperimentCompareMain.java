package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Command-line entry point for comparing experiment JSON reports as CSV. */
public final class InferenceRepairExperimentCompareMain {
    private InferenceRepairExperimentCompareMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        InferenceRepairExperimentSummaryReader reader =
                new InferenceRepairExperimentSummaryReader();
        List<String> lines = new ArrayList<>();
        lines.add(InferenceRepairExperimentSummary.csvHeader());
        for (File reportFile : arguments.reportFiles) {
            lines.add(reader.read(reportFile).toCsvRow());
        }
        writeCsv(arguments.outputFile, lines);
    }

    private static void writeCsv(File outputFile, List<String> lines) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create comparison output directory: " + parent.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not write comparison CSV: " + outputFile, e);
        }
    }

    private static final class Arguments {
        private final File outputFile;
        private final List<File> reportFiles;

        private Arguments(File outputFile, List<File> reportFiles) {
            this.outputFile = outputFile;
            this.reportFiles = reportFiles;
        }

        private static Arguments parse(String[] args) {
            File outputFile = new File("build/inference-repair-experiments/comparison.csv");
            List<File> reportFiles = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--report".equals(arg)) {
                    reportFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--report-list".equals(arg)) {
                    reportFiles.addAll(readReportList(new File(requiredValue(args, ++i, arg))));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (reportFiles.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one --report or --report-list argument is required.");
            }
            return new Arguments(outputFile, reportFiles);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static List<File> readReportList(File reportListFile) {
            try {
                List<String> lines = Files.readAllLines(reportListFile.toPath(), StandardCharsets.UTF_8);
                List<File> files = new ArrayList<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        files.add(new File(trimmed));
                    }
                }
                return files;
            } catch (IOException e) {
                throw new RuntimeException("Could not read report list: " + reportListFile, e);
            }
        }
    }
}
