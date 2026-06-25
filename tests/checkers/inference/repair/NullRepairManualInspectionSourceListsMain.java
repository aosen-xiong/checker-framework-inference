package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Writes source lists from NullRepair manual-inspection rows. */
public final class NullRepairManualInspectionSourceListsMain {
    private NullRepairManualInspectionSourceListsMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<NullRepairManualInspectionCase> cases =
                filterCases(
                        NullRepairManualInspectionParser.parse(arguments.inputFile),
                        arguments.includeProjects,
                        arguments.excludeProjects,
                        arguments.maxBestScore);
        Map<String, Set<String>> sourcesByProject = sourcesByProject(cases);
        writeSourceLists(arguments.outputDirectory, sourcesByProject);
        System.out.println(
                "Wrote "
                        + sourcesByProject.size()
                        + " NullRepair manual-inspection source list(s) to "
                        + arguments.outputDirectory.getPath()
                        + " from "
                        + cases.size()
                        + " case(s).");
    }

    private static List<NullRepairManualInspectionCase> filterCases(
            List<NullRepairManualInspectionCase> cases,
            Set<String> includeProjects,
            Set<String> excludeProjects,
            int maxBestScore) {
        List<NullRepairManualInspectionCase> filtered = new ArrayList<>();
        for (NullRepairManualInspectionCase candidate : cases) {
            String project = candidate.getBenchmark();
            if (!includeProjects.isEmpty() && !includeProjects.contains(project)) {
                continue;
            }
            if (excludeProjects.contains(project)) {
                continue;
            }
            if (candidate.bestScore() > maxBestScore) {
                continue;
            }
            filtered.add(candidate);
        }
        return filtered;
    }

    private static Map<String, Set<String>> sourcesByProject(
            List<NullRepairManualInspectionCase> cases) {
        Map<String, Set<String>> sourcesByProject = new LinkedHashMap<>();
        for (NullRepairManualInspectionCase candidate : cases) {
            Set<String> sources = sourcesByProject.get(candidate.getBenchmark());
            if (sources == null) {
                sources = new LinkedHashSet<>();
                sourcesByProject.put(candidate.getBenchmark(), sources);
            }
            sources.add(candidate.sourcePathRelativeToBenchmarkRoot());
        }
        return sourcesByProject;
    }

    private static void writeSourceLists(File outputDirectory, Map<String, Set<String>> sourcesByProject) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create source-list directory: " + outputDirectory);
        }
        for (Map.Entry<String, Set<String>> entry : sourcesByProject.entrySet()) {
            List<String> sources = new ArrayList<>(entry.getValue());
            Collections.sort(sources);
            File outputFile = new File(outputDirectory, entry.getKey() + ".txt");
            try {
                Files.write(outputFile.toPath(), sources, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not write source list: " + outputFile, e);
            }
        }
    }

    private static final class Arguments {
        private final File inputFile;
        private final File outputDirectory;
        private final Set<String> includeProjects;
        private final Set<String> excludeProjects;
        private final int maxBestScore;

        private Arguments(
                File inputFile,
                File outputDirectory,
                Set<String> includeProjects,
                Set<String> excludeProjects,
                int maxBestScore) {
            this.inputFile = inputFile;
            this.outputDirectory = outputDirectory;
            this.includeProjects = includeProjects;
            this.excludeProjects = excludeProjects;
            this.maxBestScore = maxBestScore;
        }

        private static Arguments parse(String[] args) {
            File inputFile = null;
            File outputDirectory = new File("build/inference-repair-source-lists/nullrepair-manual");
            Set<String> includeProjects = new LinkedHashSet<>();
            Set<String> excludeProjects = new LinkedHashSet<>();
            int maxBestScore = Integer.MAX_VALUE;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--input".equals(arg)) {
                    inputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out-dir".equals(arg)) {
                    outputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--include-projects".equals(arg)) {
                    includeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--exclude-projects".equals(arg)) {
                    excludeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--max-best-score".equals(arg)) {
                    maxBestScore = Integer.parseInt(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null) {
                throw new IllegalArgumentException("--input is required.");
            }
            return new Arguments(
                    inputFile, outputDirectory, includeProjects, excludeProjects, maxBestScore);
        }

        private static List<String> splitProjects(String value) {
            List<String> projects = new ArrayList<>();
            for (String project : value.split(",")) {
                String trimmed = project.trim();
                if (!trimmed.isEmpty()) {
                    projects.add(trimmed);
                }
            }
            return projects;
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
