package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Command-line entry point for writing one human-readable repair case-study trace. */
public final class InferenceRepairCaseStudyMain {
    private InferenceRepairCaseStudyMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        File outputDirectory = arguments.outputDirectory();
        deleteRecursively(outputDirectory);
        InferenceRepairExperimentResult result =
                new InferenceRepairExperimentRunner(
                                InferenceRepairExperimentConfig.unsatCoreGuided(),
                                InferenceRepairConfiguration.nninfDefault(),
                                new SimpleNninfRepairPlanner(),
                                outputDirectory,
                                new InferenceRepairEditGenerator())
                        .run(arguments.sourceFile);
        write(arguments.outputFile, new InferenceRepairCaseStudyTrace().render(result));
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            throw new IllegalStateException("Could not delete stale case-study output: " + file);
        }
    }

    private static void write(File outputFile, String trace) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create case-study output directory: " + parent.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), trace.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write case-study trace: " + outputFile, e);
        }
    }

    private static final class Arguments {
        private final File sourceFile;
        private final File outputFile;

        private Arguments(File sourceFile, File outputFile) {
            this.sourceFile = sourceFile;
            this.outputFile = outputFile;
        }

        private File outputDirectory() {
            File parent = outputFile.getParentFile();
            return parent == null
                    ? new File("build/inference-repair-case-study/work")
                    : new File(parent, "work");
        }

        private static Arguments parse(String[] args) {
            File sourceFile = null;
            File outputFile = new File("build/inference-repair-case-study/trace.md");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--source".equals(arg)) {
                    sourceFile = new File(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (sourceFile == null) {
                throw new IllegalArgumentException("Missing required --source argument.");
            }
            return new Arguments(sourceFile, outputFile);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
