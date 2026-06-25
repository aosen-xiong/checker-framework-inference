package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Writes one Java source list per immediate child project under a benchmark root. */
public final class InferenceBenchmarkSourceListsMain {
    private InferenceBenchmarkSourceListsMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<File> projects =
                filterProjects(
                        discoverProjects(arguments.benchmarkRoot),
                        arguments.includeProjects,
                        arguments.excludeProjects);
        for (File project : projects) {
            writeProjectSourceList(arguments.outputDirectory, project, arguments.sourceFilter);
        }
        System.out.println(
                "Wrote "
                        + projects.size()
                        + " benchmark source list(s) to "
                        + arguments.outputDirectory.getPath()
                        + " using filter "
                        + arguments.sourceFilter.name());
    }

    private static List<File> discoverProjects(File benchmarkRoot) {
        if (!benchmarkRoot.isDirectory()) {
            throw new IllegalArgumentException("Benchmark root does not exist: " + benchmarkRoot);
        }
        File[] children = benchmarkRoot.listFiles();
        List<File> projects = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".")) {
                    projects.add(child);
                }
            }
        }
        Collections.sort(projects);
        return projects;
    }

    private static List<File> filterProjects(
            List<File> projects, Set<String> includeProjects, Set<String> excludeProjects) {
        List<File> filtered = new ArrayList<>();
        for (File project : projects) {
            String projectName = project.getName();
            if (!includeProjects.isEmpty() && !includeProjects.contains(projectName)) {
                continue;
            }
            if (excludeProjects.contains(projectName)) {
                continue;
            }
            filtered.add(project);
        }
        return filtered;
    }

    private static void writeProjectSourceList(
            File outputDirectory,
            File project,
            InferenceRepairSourceDiscovery.SourceFilter sourceFilter) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create source-list directory: " + outputDirectory);
        }
        List<File> sources =
                InferenceRepairSourceDiscovery.discoverJavaSources(
                        Collections.singletonList(project), sourceFilter);
        List<String> relativePaths = new ArrayList<>();
        Path projectRoot = project.toPath().toAbsolutePath().normalize();
        for (File source : sources) {
            Path sourcePath = source.toPath().toAbsolutePath().normalize();
            if (sourcePath.startsWith(projectRoot)) {
                relativePaths.add(projectRoot.relativize(sourcePath).toString());
            } else {
                relativePaths.add(source.getPath());
            }
        }
        File outputFile = new File(outputDirectory, project.getName() + ".txt");
        try {
            Files.write(outputFile.toPath(), relativePaths, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not write benchmark source list: " + outputFile, e);
        }
    }

    private static final class Arguments {
        private final File benchmarkRoot;
        private final File outputDirectory;
        private final InferenceRepairSourceDiscovery.SourceFilter sourceFilter;
        private final Set<String> includeProjects;
        private final Set<String> excludeProjects;

        private Arguments(
                File benchmarkRoot,
                File outputDirectory,
                InferenceRepairSourceDiscovery.SourceFilter sourceFilter,
                Set<String> includeProjects,
                Set<String> excludeProjects) {
            this.benchmarkRoot = benchmarkRoot;
            this.outputDirectory = outputDirectory;
            this.sourceFilter = sourceFilter;
            this.includeProjects = includeProjects;
            this.excludeProjects = excludeProjects;
        }

        private static Arguments parse(String[] args) {
            File benchmarkRoot = null;
            File outputDirectory = new File("build/inference-repair-source-lists");
            InferenceRepairSourceDiscovery.SourceFilter sourceFilter =
                    InferenceRepairSourceDiscovery.SourceFilter.ALL;
            Set<String> includeProjects = new LinkedHashSet<>();
            Set<String> excludeProjects = new LinkedHashSet<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--benchmark-root".equals(arg)) {
                    benchmarkRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--out-dir".equals(arg)) {
                    outputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--source-filter".equals(arg)) {
                    sourceFilter =
                            InferenceRepairSourceDiscovery.SourceFilter.valueOf(
                                    requiredValue(args, ++i, arg));
                } else if ("--include-projects".equals(arg)) {
                    includeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--exclude-projects".equals(arg)) {
                    excludeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (benchmarkRoot == null) {
                throw new IllegalArgumentException("--benchmark-root is required.");
            }
            return new Arguments(
                    benchmarkRoot, outputDirectory, sourceFilter, includeProjects, excludeProjects);
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
