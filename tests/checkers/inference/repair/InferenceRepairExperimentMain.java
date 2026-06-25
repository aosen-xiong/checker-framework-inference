package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Command-line entry point for writing inference-repair experiment JSON reports. */
public final class InferenceRepairExperimentMain {
    private InferenceRepairExperimentMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        if (arguments.editProviderMode != InferenceRepairEditProviderMode.DETERMINISTIC_ONLY
                && arguments.mode != InferenceRepairExperimentMode.NO_REPAIR) {
            throw new IllegalArgumentException(
                    "CLI experiments currently support only deterministic edit providers. "
                            + "Use tests or a custom harness to supply an AI repair client.");
        }

        InferenceRepairExperimentConfig experimentConfig =
                new InferenceRepairExperimentConfig(arguments.mode, arguments.editProviderMode);
        InferenceRepairExperimentBatchResult result =
                new InferenceRepairExperimentRunner(
                                experimentConfig,
                                InferenceRepairConfiguration.nninfDefault(),
                                new SimpleNninfRepairPlanner(),
                        arguments.outputDirectory(),
                        new InferenceRepairEditGenerator())
                        .runAll(arguments.metadata(), arguments.sourceFiles);
        writeReport(arguments.outputFile, result.toJson());
    }

    private static void writeReport(File outputFile, String json) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create report output directory: " + parent.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write experiment report: " + outputFile, e);
        }
    }

    private static final class Arguments {
        private final InferenceRepairExperimentMode mode;
        private final InferenceRepairEditProviderMode editProviderMode;
        private final String projectName;
        private final File projectRoot;
        private final String revision;
        private final File outputFile;
        private final List<File> sourceFiles;

        private Arguments(
                InferenceRepairExperimentMode mode,
                InferenceRepairEditProviderMode editProviderMode,
                String projectName,
                File projectRoot,
                String revision,
                File outputFile,
                List<File> sourceFiles) {
            this.mode = mode;
            this.editProviderMode = editProviderMode;
            this.projectName = projectName;
            this.projectRoot = projectRoot;
            this.revision = revision;
            this.outputFile = outputFile;
            this.sourceFiles = sourceFiles;
        }

        private InferenceRepairExperimentMetadata metadata() {
            return new InferenceRepairExperimentMetadata(projectName, projectRoot, revision);
        }

        private File outputDirectory() {
            File parent = outputFile.getParentFile();
            return parent == null
                    ? new File("build/inference-repair-experiments")
                    : new File(parent, "work");
        }

        private static Arguments parse(String[] args) {
            InferenceRepairExperimentMode mode = InferenceRepairExperimentMode.UNSAT_CORE_GUIDED;
            InferenceRepairEditProviderMode editProviderMode =
                    InferenceRepairEditProviderMode.DETERMINISTIC_ONLY;
            String projectName = "";
            File projectRoot = null;
            String revision = "";
            File outputFile = new File("build/inference-repair-experiments/report.json");
            List<File> sourceFiles = new ArrayList<>();
            List<File> sourceListFiles = new ArrayList<>();
            List<File> sourceRoots = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--mode".equals(arg)) {
                    mode = InferenceRepairExperimentMode.valueOf(requiredValue(args, ++i, arg));
                } else if ("--edit-provider-mode".equals(arg)) {
                    editProviderMode =
                            InferenceRepairEditProviderMode.valueOf(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--project-name".equals(arg)) {
                    projectName = requiredValue(args, ++i, arg);
                } else if ("--project-root".equals(arg)) {
                    projectRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--revision".equals(arg)) {
                    revision = requiredValue(args, ++i, arg);
                } else if ("--source".equals(arg)) {
                    sourceFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--source-list".equals(arg)) {
                    sourceListFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--source-root".equals(arg)) {
                    sourceRoots.add(new File(requiredValue(args, ++i, arg)));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            for (File sourceListFile : sourceListFiles) {
                sourceFiles.addAll(
                        InferenceRepairSourceDiscovery.readSourceList(sourceListFile, projectRoot));
            }
            sourceFiles.addAll(InferenceRepairSourceDiscovery.discoverJavaSources(sourceRoots));
            if (sourceFiles.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one --source, --source-list, or --source-root argument is required.");
            }
            return new Arguments(
                    mode, editProviderMode, projectName, projectRoot, revision, outputFile, sourceFiles);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
