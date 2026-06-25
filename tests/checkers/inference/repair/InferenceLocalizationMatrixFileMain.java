package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Writes a pilot-matrix CSV from a directory of per-project source lists. */
public final class InferenceLocalizationMatrixFileMain {
    private InferenceLocalizationMatrixFileMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<File> sourceLists =
                filterSourceLists(
                        discoverSourceLists(arguments.sourceListDirectory),
                        arguments.includeProjects,
                        arguments.excludeProjects);
        writeMatrix(
                arguments.outputFile,
                arguments.benchmarkRoot,
                arguments.companionSourceListDirectory,
                sourceLists);
        System.out.println(
                "Wrote "
                        + sourceLists.size()
                        + " project row(s) to "
                        + arguments.outputFile.getPath());
    }

    private static List<File> discoverSourceLists(File sourceListDirectory) {
        if (!sourceListDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Source-list directory does not exist: " + sourceListDirectory);
        }
        File[] children = sourceListDirectory.listFiles();
        List<File> sourceLists = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                if (child.isFile() && child.getName().endsWith(".txt")) {
                    sourceLists.add(child);
                }
            }
        }
        Collections.sort(sourceLists);
        return sourceLists;
    }

    private static List<File> filterSourceLists(
            List<File> sourceLists, Set<String> includeProjects, Set<String> excludeProjects) {
        List<File> filtered = new ArrayList<>();
        for (File sourceList : sourceLists) {
            String projectName = projectName(sourceList);
            if (!includeProjects.isEmpty() && !includeProjects.contains(projectName)) {
                continue;
            }
            if (excludeProjects.contains(projectName)) {
                continue;
            }
            filtered.add(sourceList);
        }
        return filtered;
    }

    private static void writeMatrix(
            File outputFile,
            File benchmarkRoot,
            File companionSourceListDirectory,
            List<File> sourceLists) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create matrix directory: " + parent);
        }
        StringBuilder csv = new StringBuilder();
        csv.append("# projectName,sourceList,projectRoot[,companionSourceList]\n");
        for (File sourceList : sourceLists) {
            String projectName = projectName(sourceList);
            File projectRoot = benchmarkRoot == null ? null : new File(benchmarkRoot, projectName);
            File companionSourceList =
                    companionSourceListDirectory == null
                            ? null
                            : new File(companionSourceListDirectory, sourceList.getName());
            csv.append(csv(projectName))
                    .append(",")
                    .append(csv(sourceList.getPath()))
                    .append(",");
            if (projectRoot != null) {
                csv.append(csv(projectRoot.getPath()));
            }
            if (companionSourceList != null && companionSourceList.isFile()) {
                csv.append(",").append(csv(companionSourceList.getPath()));
            }
            csv.append("\n");
        }
        try {
            Files.write(outputFile.toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write localization matrix file: " + outputFile, e);
        }
    }

    private static String projectName(File sourceList) {
        String name = sourceList.getName();
        return name.endsWith(".txt") ? name.substring(0, name.length() - ".txt".length()) : name;
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static final class Arguments {
        private final File sourceListDirectory;
        private final File benchmarkRoot;
        private final File companionSourceListDirectory;
        private final File outputFile;
        private final Set<String> includeProjects;
        private final Set<String> excludeProjects;

        private Arguments(
                File sourceListDirectory,
                File benchmarkRoot,
                File companionSourceListDirectory,
                File outputFile,
                Set<String> includeProjects,
                Set<String> excludeProjects) {
            this.sourceListDirectory = sourceListDirectory;
            this.benchmarkRoot = benchmarkRoot;
            this.companionSourceListDirectory = companionSourceListDirectory;
            this.outputFile = outputFile;
            this.includeProjects = includeProjects;
            this.excludeProjects = excludeProjects;
        }

        private static Arguments parse(String[] args) {
            File sourceListDirectory = null;
            File benchmarkRoot = null;
            File companionSourceListDirectory = null;
            File outputFile = new File("build/inference-repair-source-lists/matrix.csv");
            Set<String> includeProjects = new LinkedHashSet<>();
            Set<String> excludeProjects = new LinkedHashSet<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--source-list-dir".equals(arg)) {
                    sourceListDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--benchmark-root".equals(arg)) {
                    benchmarkRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--companion-source-list-dir".equals(arg)) {
                    companionSourceListDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--include-projects".equals(arg)) {
                    includeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--exclude-projects".equals(arg)) {
                    excludeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (sourceListDirectory == null) {
                throw new IllegalArgumentException("--source-list-dir is required.");
            }
            return new Arguments(
                    sourceListDirectory,
                    benchmarkRoot,
                    companionSourceListDirectory,
                    outputFile,
                    includeProjects,
                    excludeProjects);
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
