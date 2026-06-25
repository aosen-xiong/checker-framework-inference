package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Runs localization pilot reports for a matrix of benchmark project source lists. */
public final class InferenceLocalizationPilotMatrixMain {
    private InferenceLocalizationPilotMatrixMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<Project> projects = readProjects(arguments.matrixFile);
        List<ProjectSummary> summaries = new ArrayList<>();
        for (Project project : projects) {
            summaries.add(runProject(arguments, project));
        }
        writeAggregateSummary(new File(arguments.outputDirectory, "matrix-summary.csv"), summaries);
        System.out.println(
                "Completed localization pilot matrix for "
                        + projects.size()
                        + " project(s) in "
                        + arguments.outputDirectory.getPath());
    }

    private static ProjectSummary runProject(Arguments arguments, Project project) {
        File jsonOutput = new File(arguments.outputDirectory, project.name + ".json");
        File csvOutput = new File(arguments.outputDirectory, project.name + ".csv");
        File summaryOutput = new File(arguments.outputDirectory, project.name + "-summary.md");
        List<String> reportArgs = new ArrayList<>();
        reportArgs.add("--out");
        reportArgs.add(jsonOutput.getPath());
        reportArgs.add("--csv-out");
        reportArgs.add(csvOutput.getPath());
        reportArgs.add("--source-list");
        reportArgs.add(project.sourceList.getPath());
        if (project.companionSourceList != null) {
            reportArgs.add("--companion-source-list");
            reportArgs.add(project.companionSourceList.getPath());
        }
        reportArgs.add("--project-name");
        reportArgs.add(project.name);
        reportArgs.add("--sample-size");
        reportArgs.add(String.valueOf(arguments.sampleSize));
        reportArgs.add("--seed");
        reportArgs.add(String.valueOf(arguments.seed));
        reportArgs.add("--top-k");
        reportArgs.add(String.valueOf(arguments.topK));
        reportArgs.add("--timeout-seconds");
        reportArgs.add(String.valueOf(arguments.timeoutSeconds));
        if (arguments.validateSourceRepairPlans) {
            reportArgs.add("--validate-source-repair-plans");
        }
        if (project.projectRoot != null) {
            reportArgs.add("--project-root");
            reportArgs.add(project.projectRoot.getPath());
        }
        InferenceLocalizationStudyMain.main(reportArgs.toArray(new String[reportArgs.size()]));
        InferenceLocalizationStudySummaryMain.main(
                new String[] {"--csv", csvOutput.getPath(), "--out", summaryOutput.getPath()});
        return ProjectSummary.from(project.name, csvOutput);
    }

    private static void writeAggregateSummary(File outputFile, List<ProjectSummary> summaries) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create matrix summary directory: " + parent);
        }
        StringBuilder csv = new StringBuilder();
        csv.append("project,total,sat,unsat,runErrors,timeouts,unsatYield,timeoutRate,")
                .append("candidateYield,sourceRealizableCandidateYield,sourceRealizableYield,")
                .append("repairPlanMaterialized,repairPlanAttempted,")
                .append("repairPlanInferenceSolved,repairPlanVerified,")
                .append("repairFollowUpAttempted,repairFollowUpVerified,")
                .append("repairPlanMaterializationYield,repairPlanAttemptYield,")
                .append("repairPlanInferenceSolvedYield,")
                .append("repairPlanVerifiedYield,repairFollowUpVerifiedYield\n");
        for (ProjectSummary summary : summaries) {
            csv.append(csv(summary.projectName))
                    .append(",")
                    .append(summary.total)
                    .append(",")
                    .append(summary.sat)
                    .append(",")
                    .append(summary.unsat)
                    .append(",")
                    .append(summary.runErrors)
                    .append(",")
                    .append(summary.timeouts)
                    .append(",")
                    .append(csv(percent(summary.unsat, summary.total)))
                    .append(",")
                    .append(csv(percent(summary.timeouts, summary.total)))
                    .append(",")
                    .append(csv(percent(summary.unsatWithCandidates, summary.unsat)))
                    .append(",")
                    .append(csv(percent(summary.unsatWithSourceRealizableCandidates, summary.unsat)))
                    .append(",")
                    .append(csv(percent(summary.unsatSourceRealizable, summary.unsat)))
                    .append(",")
                    .append(summary.repairPlanMaterialized)
                    .append(",")
                    .append(summary.repairPlanAttempted)
                    .append(",")
                    .append(summary.repairPlanInferenceSolved)
                    .append(",")
                    .append(summary.repairPlanVerified)
                    .append(",")
                    .append(summary.repairFollowUpAttempted)
                    .append(",")
                    .append(summary.repairFollowUpVerified)
                    .append(",")
                    .append(csv(percent(summary.repairPlanMaterialized, summary.unsat)))
                    .append(",")
                    .append(csv(percent(summary.repairPlanAttempted, summary.unsat)))
                    .append(",")
                    .append(csv(percent(summary.repairPlanInferenceSolved, summary.repairPlanAttempted)))
                    .append(",")
                    .append(csv(percent(summary.repairPlanVerified, summary.repairPlanAttempted)))
                    .append(",")
                    .append(csv(percent(summary.repairFollowUpVerified, summary.repairFollowUpAttempted)))
                    .append("\n");
        }
        try {
            Files.write(outputFile.toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write matrix summary: " + outputFile, e);
        }
    }

    private static List<Project> readProjects(File matrixFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(matrixFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read localization matrix: " + matrixFile, e);
        }
        List<Project> projects = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            List<String> fields = parseCsvLine(trimmed);
            if (fields.size() < 2 || fields.size() > 4) {
                throw new IllegalArgumentException(
                        "Expected projectName,sourceList[,projectRoot[,companionSourceList]] in "
                                + matrixFile
                                + ": "
                                + line);
            }
            File projectRoot = fields.size() == 3 && !fields.get(2).isEmpty() ? new File(fields.get(2)) : null;
            if (fields.size() == 4 && !fields.get(2).isEmpty()) {
                projectRoot = new File(fields.get(2));
            }
            File companionSourceList =
                    fields.size() == 4 && !fields.get(3).isEmpty() ? new File(fields.get(3)) : null;
            projects.add(
                    new Project(fields.get(0), new File(fields.get(1)), projectRoot, companionSourceList));
        }
        return projects;
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

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String percent(int numerator, int denominator) {
        if (denominator == 0) {
            return "n/a";
        }
        return Math.round((1000.0 * numerator) / denominator) / 10.0 + "%";
    }

    private static final class ProjectSummary {
        private final String projectName;
        private final int total;
        private final int sat;
        private final int unsat;
        private final int runErrors;
        private final int timeouts;
        private final int unsatWithCandidates;
        private final int unsatWithSourceRealizableCandidates;
        private final int unsatSourceRealizable;
        private final int repairPlanMaterialized;
        private final int repairPlanAttempted;
        private final int repairPlanInferenceSolved;
        private final int repairPlanVerified;
        private final int repairFollowUpAttempted;
        private final int repairFollowUpVerified;

        private ProjectSummary(
                String projectName,
                int total,
                int sat,
                int unsat,
                int runErrors,
                int timeouts,
                int unsatWithCandidates,
                int unsatWithSourceRealizableCandidates,
                int unsatSourceRealizable,
                int repairPlanMaterialized,
                int repairPlanAttempted,
                int repairPlanInferenceSolved,
                int repairPlanVerified,
                int repairFollowUpAttempted,
                int repairFollowUpVerified) {
            this.projectName = projectName;
            this.total = total;
            this.sat = sat;
            this.unsat = unsat;
            this.runErrors = runErrors;
            this.timeouts = timeouts;
            this.unsatWithCandidates = unsatWithCandidates;
            this.unsatWithSourceRealizableCandidates = unsatWithSourceRealizableCandidates;
            this.unsatSourceRealizable = unsatSourceRealizable;
            this.repairPlanMaterialized = repairPlanMaterialized;
            this.repairPlanAttempted = repairPlanAttempted;
            this.repairPlanInferenceSolved = repairPlanInferenceSolved;
            this.repairPlanVerified = repairPlanVerified;
            this.repairFollowUpAttempted = repairFollowUpAttempted;
            this.repairFollowUpVerified = repairFollowUpVerified;
        }

        private static ProjectSummary from(String projectName, File csvFile) {
            List<String> lines;
            try {
                lines = Files.readAllLines(csvFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not read localization CSV: " + csvFile, e);
            }
            if (lines.isEmpty()) {
                return new ProjectSummary(projectName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            List<String> headers = parseCsvLine(lines.get(0));
            int runErrorIndex = headers.indexOf("runError");
            int solvedIndex = headers.indexOf("solverHadSolution");
            int top1LocationsIndex = headers.indexOf("top1Locations");
            int sat = 0;
            int unsat = 0;
            int runErrors = 0;
            int timeouts = 0;
            int unsatWithCandidates = 0;
            int unsatWithSourceRealizableCandidates = 0;
            int unsatSourceRealizable = 0;
            int repairPlanMaterialized = 0;
            int repairPlanAttempted = 0;
            int repairPlanInferenceSolved = 0;
            int repairPlanVerified = 0;
            int repairFollowUpAttempted = 0;
            int repairFollowUpVerified = 0;
            int total = 0;
            for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                if (line.trim().isEmpty()) {
                    continue;
                }
                total++;
                List<String> values = parseCsvLine(line);
                String runError = valueAt(values, runErrorIndex);
                boolean hasRunError = !runError.isEmpty();
                if (hasRunError) {
                    runErrors++;
                    if (runError.startsWith("TIMEOUT")) {
                        timeouts++;
                    }
                }
                if ("true".equals(valueAt(values, solvedIndex))) {
                    sat++;
                } else if (!hasRunError) {
                    unsat++;
                    if (!valueAt(values, top1LocationsIndex).isEmpty()) {
                        unsatWithCandidates++;
                    }
                    if ("true".equals(
                            valueForHeader(
                                    headers, values, "top1HasSourceRealizableRepairUnit"))) {
                        unsatWithSourceRealizableCandidates++;
                    }
                    if ("true".equals(valueForHeader(headers, values, "hasSourceRealizableRepairUnit"))) {
                        unsatSourceRealizable++;
                    }
                }
                if ("true".equals(valueForHeader(headers, values, "sourceRepairPlanMaterialized"))) {
                    repairPlanMaterialized++;
                }
                if (repairPlanAttempted(headers, values)) {
                    repairPlanAttempted++;
                }
                if ("true".equals(valueForHeader(headers, values, "sourceRepairPlanInferenceSolved"))) {
                    repairPlanInferenceSolved++;
                }
                if ("true".equals(valueForHeader(headers, values, "sourceRepairPlanVerified"))) {
                    repairPlanVerified++;
                }
                if ("true".equals(valueForHeader(headers, values, "sourceRepairFollowUpAttempted"))) {
                    repairFollowUpAttempted++;
                }
                if ("true".equals(valueForHeader(headers, values, "sourceRepairFollowUpVerified"))) {
                    repairFollowUpVerified++;
                }
            }
            return new ProjectSummary(
                    projectName,
                    total,
                    sat,
                    unsat,
                    runErrors,
                    timeouts,
                    unsatWithCandidates,
                    unsatWithSourceRealizableCandidates,
                    unsatSourceRealizable,
                    repairPlanMaterialized,
                    repairPlanAttempted,
                    repairPlanInferenceSolved,
                    repairPlanVerified,
                    repairFollowUpAttempted,
                    repairFollowUpVerified);
        }

        private static String valueAt(List<String> values, int index) {
            return index >= 0 && index < values.size() ? values.get(index) : "";
        }

        private static boolean repairPlanAttempted(List<String> headers, List<String> values) {
            return "true".equals(valueForHeader(headers, values, "sourceRepairPlanMaterialized"))
                    || "true".equals(valueForHeader(headers, values, "sourceRepairPlanInferenceSolved"))
                    || "true".equals(valueForHeader(headers, values, "sourceRepairPlanVerified"))
                    || "true".equals(valueForHeader(headers, values, "sourceRepairFollowUpAttempted"));
        }

        private static String valueForHeader(
                List<String> headers, List<String> values, String header) {
            return valueAt(values, headers.indexOf(header));
        }
    }

    private static final class Project {
        private final String name;
        private final File sourceList;
        private final File projectRoot;
        private final File companionSourceList;

        private Project(
                String name, File sourceList, File projectRoot, File companionSourceList) {
            this.name = name;
            this.sourceList = sourceList;
            this.projectRoot = projectRoot;
            this.companionSourceList = companionSourceList;
        }
    }

    private static final class Arguments {
        private final File matrixFile;
        private final File outputDirectory;
        private final int sampleSize;
        private final long seed;
        private final int topK;
        private final int timeoutSeconds;
        private final boolean validateSourceRepairPlans;

        private Arguments(
                File matrixFile,
                File outputDirectory,
                int sampleSize,
                long seed,
                int topK,
                int timeoutSeconds,
                boolean validateSourceRepairPlans) {
            this.matrixFile = matrixFile;
            this.outputDirectory = outputDirectory;
            this.sampleSize = sampleSize;
            this.seed = seed;
            this.topK = topK;
            this.timeoutSeconds = timeoutSeconds;
            this.validateSourceRepairPlans = validateSourceRepairPlans;
        }

        private static Arguments parse(String[] args) {
            File matrixFile = null;
            File outputDirectory = new File("build/inference-localization-study/matrix");
            int sampleSize = 20;
            long seed = 1L;
            int topK = 5;
            int timeoutSeconds = 30;
            boolean validateSourceRepairPlans = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--matrix".equals(arg)) {
                    matrixFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out-dir".equals(arg)) {
                    outputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--sample-size".equals(arg)) {
                    sampleSize = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--seed".equals(arg)) {
                    seed = Long.parseLong(requiredValue(args, ++i, arg));
                } else if ("--top-k".equals(arg)) {
                    topK = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--timeout-seconds".equals(arg)) {
                    timeoutSeconds = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--validate-source-repair-plans".equals(arg)) {
                    validateSourceRepairPlans = true;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (matrixFile == null) {
                throw new IllegalArgumentException("--matrix is required.");
            }
            return new Arguments(
                    matrixFile,
                    outputDirectory,
                    sampleSize,
                    seed,
                    topK,
                    timeoutSeconds,
                    validateSourceRepairPlans);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
