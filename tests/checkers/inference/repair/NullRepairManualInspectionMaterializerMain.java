package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Materializes source files referenced by NullRepair manual-inspection rows. */
public final class NullRepairManualInspectionMaterializerMain {
    private static final String DEFAULT_ARTIFACT_BENCHMARKS_URL =
            "https://anonymous.4open.science/api/repo/"
                    + "anonymous_submission_nullrepair-6B09/file/benchmarks";

    private NullRepairManualInspectionMaterializerMain() {}

    private static final JavaSourceClosureScanner CLOSURE_SCANNER = new JavaSourceClosureScanner();

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<NullRepairManualInspectionCase> cases =
                filterCases(
                        NullRepairManualInspectionParser.parse(arguments.inputFile),
                        arguments.includeProjects,
                        arguments.excludeProjects,
                        arguments.maxBestScore);
        Map<String, Set<String>> sourcesByProject = sourcesByProject(cases);
        Map<String, Set<String>> targetSourcesByProject = copySources(sourcesByProject);
        materialize(arguments, sourcesByProject);
        if (arguments.closureDepth > 0) {
            materializeClosure(arguments, sourcesByProject);
        }
        writeSourceLists(arguments.sourceListOutputDirectory, sourcesByProject);
        if (arguments.targetSourceListOutputDirectory != null) {
            writeSourceLists(arguments.targetSourceListOutputDirectory, targetSourcesByProject);
        }
        writeCaseManifest(arguments.caseManifestOutputFile, cases);
        System.out.println(
                "Materialized "
                        + sourceCount(sourcesByProject)
                        + " source file(s) for "
                        + sourcesByProject.size()
                        + " project(s) into "
                        + arguments.outputRoot.getPath());
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

    private static Map<String, Set<String>> copySources(Map<String, Set<String>> sourcesByProject) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : sourcesByProject.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        return copy;
    }

    private static void materialize(Arguments arguments, Map<String, Set<String>> sourcesByProject) {
        for (Map.Entry<String, Set<String>> entry : sourcesByProject.entrySet()) {
            for (String source : entry.getValue()) {
                if (!materializeOne(arguments, entry.getKey(), source, false)) {
                    throw new RuntimeException(
                            "Could not materialize " + entry.getKey() + "/" + source);
                }
            }
        }
    }

    private static void materializeClosure(
            Arguments arguments, Map<String, Set<String>> sourcesByProject) {
        for (Map.Entry<String, Set<String>> entry : sourcesByProject.entrySet()) {
            Set<String> knownSources = entry.getValue();
            Set<String> frontier = new LinkedHashSet<>(knownSources);
            Set<String> scanned = new HashSet<>();
            for (int depth = 0; depth < arguments.closureDepth && !frontier.isEmpty(); depth++) {
                Set<String> nextFrontier = new LinkedHashSet<>();
                for (String source : frontier) {
                    if (!scanned.add(source)) {
                        continue;
                    }
                    File materialized = new File(new File(arguments.outputRoot, entry.getKey()), source);
                    if (!materialized.isFile()) {
                        continue;
                    }
                    for (String dependency : CLOSURE_SCANNER.likelyDependencies(materialized, source)) {
                        if (knownSources.contains(dependency)) {
                            continue;
                        }
                        if (materializeOne(arguments, entry.getKey(), dependency, true)) {
                            knownSources.add(dependency);
                            nextFrontier.add(dependency);
                        }
                    }
                }
                frontier = nextFrontier;
            }
        }
    }

    private static boolean materializeOne(
            Arguments arguments, String project, String source, boolean allowMissing) {
        File outputFile = new File(new File(arguments.outputRoot, project), source);
        ensureParent(outputFile);
        try {
            if (arguments.benchmarkRoot != null) {
                File inputFile = new File(new File(arguments.benchmarkRoot, project), source);
                if (!inputFile.isFile()) {
                    return false;
                }
                Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                URL inputUrl = sourceUrl(arguments.artifactBenchmarksUrl, project, source);
                try (InputStream input = inputUrl.openStream()) {
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return true;
        } catch (IOException e) {
            if (allowMissing) {
                return false;
            }
            throw new RuntimeException("Could not materialize " + project + "/" + source, e);
        }
    }

    private static URL sourceUrl(String artifactBenchmarksUrl, String project, String source)
            throws IOException {
        String base =
                artifactBenchmarksUrl.endsWith("/")
                        ? artifactBenchmarksUrl.substring(0, artifactBenchmarksUrl.length() - 1)
                        : artifactBenchmarksUrl;
        return new URL(base + "/" + encodePath(project + "/" + source));
    }

    private static String encodePath(String path) throws IOException {
        StringBuilder encoded = new StringBuilder();
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append("/");
            }
            encoded.append(URLEncoder.encode(segments[i], "UTF-8").replace("+", "%20"));
        }
        return encoded.toString();
    }

    private static void writeSourceLists(
            File outputDirectory, Map<String, Set<String>> sourcesByProject) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create source-list directory: " + outputDirectory);
        }
        for (Map.Entry<String, Set<String>> entry : sourcesByProject.entrySet()) {
            List<String> sources = new ArrayList<>(entry.getValue());
            Collections.sort(sources);
            File outputFile = new File(outputDirectory, entry.getKey() + ".txt");
            ensureParent(outputFile);
            try {
                Files.write(outputFile.toPath(), sources, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not write source list: " + outputFile, e);
            }
        }
    }

    private static void writeCaseManifest(
            File outputFile, List<NullRepairManualInspectionCase> cases) {
        ensureParent(outputFile);
        StringBuilder csv = new StringBuilder();
        csv.append(
                "benchmark,id,type,message,path,line,relativeSource,expression,bestScore,scoreA,scoreB,scoreC\n");
        for (NullRepairManualInspectionCase candidate : cases) {
            csv.append(csv(candidate.getBenchmark()))
                    .append(",")
                    .append(csv(candidate.getId()))
                    .append(",")
                    .append(csv(candidate.getType()))
                    .append(",")
                    .append(csv(candidate.getMessage()))
                    .append(",")
                    .append(csv(candidate.getPath()))
                    .append(",")
                    .append(candidate.lineNumber())
                    .append(",")
                    .append(csv(candidate.sourcePathRelativeToBenchmarkRoot()))
                    .append(",")
                    .append(csv(candidate.getExpression()))
                    .append(",")
                    .append(candidate.bestScore())
                    .append(",")
                    .append(candidate.getScoreA())
                    .append(",")
                    .append(candidate.getScoreB())
                    .append(",")
                    .append(candidate.getScoreC())
                    .append("\n");
        }
        try {
            Files.write(outputFile.toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write case manifest: " + outputFile, e);
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static int sourceCount(Map<String, Set<String>> sourcesByProject) {
        int count = 0;
        for (Set<String> sources : sourcesByProject.values()) {
            count += sources.size();
        }
        return count;
    }

    private static void ensureParent(File outputFile) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + parent);
        }
    }

    private static final class Arguments {
        private final File inputFile;
        private final File outputRoot;
        private final File sourceListOutputDirectory;
        private final File targetSourceListOutputDirectory;
        private final File caseManifestOutputFile;
        private final File benchmarkRoot;
        private final String artifactBenchmarksUrl;
        private final Set<String> includeProjects;
        private final Set<String> excludeProjects;
        private final int maxBestScore;
        private final int closureDepth;

        private Arguments(
                File inputFile,
                File outputRoot,
                File sourceListOutputDirectory,
                File targetSourceListOutputDirectory,
                File caseManifestOutputFile,
                File benchmarkRoot,
                String artifactBenchmarksUrl,
                Set<String> includeProjects,
                Set<String> excludeProjects,
                int maxBestScore,
                int closureDepth) {
            this.inputFile = inputFile;
            this.outputRoot = outputRoot;
            this.sourceListOutputDirectory = sourceListOutputDirectory;
            this.targetSourceListOutputDirectory = targetSourceListOutputDirectory;
            this.caseManifestOutputFile = caseManifestOutputFile;
            this.benchmarkRoot = benchmarkRoot;
            this.artifactBenchmarksUrl = artifactBenchmarksUrl;
            this.includeProjects = includeProjects;
            this.excludeProjects = excludeProjects;
            this.maxBestScore = maxBestScore;
            this.closureDepth = closureDepth;
        }

        private static Arguments parse(String[] args) {
            File inputFile = null;
            File outputRoot = new File("build/nullrepair-manual-materialized/benchmarks");
            File sourceListOutputDirectory =
                    new File("build/nullrepair-manual-materialized/source-lists");
            File targetSourceListOutputDirectory = null;
            File caseManifestOutputFile =
                    new File("build/nullrepair-manual-materialized/case-manifest.csv");
            File benchmarkRoot = null;
            String artifactBenchmarksUrl = DEFAULT_ARTIFACT_BENCHMARKS_URL;
            boolean useArtifactUrl = false;
            Set<String> includeProjects = new LinkedHashSet<>();
            Set<String> excludeProjects = new LinkedHashSet<>();
            int maxBestScore = Integer.MAX_VALUE;
            int closureDepth = 0;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--input".equals(arg)) {
                    inputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out-root".equals(arg)) {
                    outputRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--source-list-out-dir".equals(arg)) {
                    sourceListOutputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--target-source-list-out-dir".equals(arg)) {
                    targetSourceListOutputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--case-manifest-out".equals(arg)) {
                    caseManifestOutputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--benchmark-root".equals(arg)) {
                    benchmarkRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--artifact-benchmarks-url".equals(arg)) {
                    artifactBenchmarksUrl = requiredValue(args, ++i, arg);
                    useArtifactUrl = true;
                } else if ("--use-default-artifact-url".equals(arg)) {
                    useArtifactUrl = true;
                } else if ("--include-projects".equals(arg)) {
                    includeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--exclude-projects".equals(arg)) {
                    excludeProjects.addAll(splitProjects(requiredValue(args, ++i, arg)));
                } else if ("--max-best-score".equals(arg)) {
                    maxBestScore = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--closure-depth".equals(arg)) {
                    closureDepth = Integer.parseInt(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null) {
                throw new IllegalArgumentException("--input is required.");
            }
            if (benchmarkRoot != null && useArtifactUrl) {
                throw new IllegalArgumentException(
                        "Use either --benchmark-root or --artifact-benchmarks-url, not both.");
            }
            if (benchmarkRoot == null && !useArtifactUrl) {
                throw new IllegalArgumentException(
                        "Set --benchmark-root or --use-default-artifact-url.");
            }
            return new Arguments(
                    inputFile,
                    outputRoot,
                    sourceListOutputDirectory,
                    targetSourceListOutputDirectory,
                    caseManifestOutputFile,
                    benchmarkRoot,
                    artifactBenchmarksUrl,
                    includeProjects,
                    excludeProjects,
                    maxBestScore,
                    closureDepth);
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
