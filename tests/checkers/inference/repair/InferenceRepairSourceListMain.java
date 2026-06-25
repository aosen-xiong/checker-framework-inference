package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Command-line entry point for writing reproducible source lists for repair experiments. */
public final class InferenceRepairSourceListMain {
    private InferenceRepairSourceListMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<File> sources =
                InferenceRepairSourceDiscovery.discoverJavaSources(
                        arguments.sourceRoots, arguments.sourceFilter);
        writeSourceList(arguments.outputFile, arguments.relativeTo, sources);
        System.out.println(
                "Wrote "
                        + sources.size()
                        + " Java source paths to "
                        + arguments.outputFile.getPath()
                        + " using filter "
                        + arguments.sourceFilter.name());
    }

    private static void writeSourceList(File outputFile, File relativeTo, List<File> sources) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create source-list directory: " + parent);
        }
        Path relativeRoot = relativeTo == null ? null : relativeTo.toPath().toAbsolutePath().normalize();
        List<String> lines = new ArrayList<>();
        for (File source : sources) {
            Path path = source.toPath().toAbsolutePath().normalize();
            if (relativeRoot != null && path.startsWith(relativeRoot)) {
                lines.add(relativeRoot.relativize(path).toString());
            } else {
                lines.add(source.getPath());
            }
        }
        try {
            Files.write(outputFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not write source list: " + outputFile, e);
        }
    }

    private static final class Arguments {
        private final File outputFile;
        private final File relativeTo;
        private final InferenceRepairSourceDiscovery.SourceFilter sourceFilter;
        private final List<File> sourceRoots;

        private Arguments(
                File outputFile,
                File relativeTo,
                InferenceRepairSourceDiscovery.SourceFilter sourceFilter,
                List<File> sourceRoots) {
            this.outputFile = outputFile;
            this.relativeTo = relativeTo;
            this.sourceFilter = sourceFilter;
            this.sourceRoots = sourceRoots;
        }

        private static Arguments parse(String[] args) {
            File outputFile = new File("build/inference-repair-source-lists/sources.txt");
            File relativeTo = null;
            InferenceRepairSourceDiscovery.SourceFilter sourceFilter =
                    InferenceRepairSourceDiscovery.SourceFilter.ALL;
            List<File> sourceRoots = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--relative-to".equals(arg)) {
                    relativeTo = new File(requiredValue(args, ++i, arg));
                } else if ("--source-filter".equals(arg)) {
                    sourceFilter =
                            InferenceRepairSourceDiscovery.SourceFilter.valueOf(
                                    requiredValue(args, ++i, arg));
                } else if ("--source-root".equals(arg)) {
                    sourceRoots.add(new File(requiredValue(args, ++i, arg)));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("At least one --source-root argument is required.");
            }
            return new Arguments(outputFile, relativeTo, sourceFilter, sourceRoots);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
