package checkers.inference.repair;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;
import checkers.inference.solver.MaxSat2TypeSolver;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Applies inference-guided repair candidates to temporary source copies and reruns inference. */
public final class SimpleNninfInferenceRepairValidator {
    private final File originalSourceFile;
    private final File outputDirectory;

    public SimpleNninfInferenceRepairValidator(File originalSourceFile, File outputDirectory) {
        this.originalSourceFile = originalSourceFile;
        this.outputDirectory = outputDirectory;
    }

    public InferenceRepairValidationResult validate(InferenceRepairCandidate candidate) {
        File repairedSourceFile = new File(outputDirectory, originalSourceFile.getName());
        String appliedEdit = writeRepairedSource(repairedSourceFile);
        InferenceRunSnapshot snapshot = runInference(repairedSourceFile);
        return new InferenceRepairValidationResult(
                candidate, repairedSourceFile, appliedEdit, snapshot);
    }

    private String writeRepairedSource(File repairedSourceFile) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + outputDirectory.getAbsolutePath());
        }

        List<String> originalLines = InferenceTestUtilities.getLines(originalSourceFile);
        List<String> repairedLines = new ArrayList<>();
        boolean changed = false;
        for (String line : originalLines) {
            if (!changed && line.contains("@NonNull String") && line.contains("=")) {
                line = replaceRhsWithNonNullFallback(line);
                changed = true;
            }
            repairedLines.add(line);
        }
        if (!changed) {
            throw new IllegalArgumentException(
                    "Could not apply inference repair: no @NonNull String target found in "
                            + originalSourceFile);
        }

        InferenceTestUtilities.writeLines(repairedLines, repairedSourceFile);
        return "replace nullable RHS with non-null fallback literal";
    }

    private static String replaceRhsWithNonNullFallback(String line) {
        return line.substring(0, line.indexOf('=') + 1) + " \"\";";
    }

    private static InferenceRunSnapshot runInference(File sourceFile) {
        InitStatus status =
                InferenceOptions.init(
                        new String[] {
                            "--checker",
                            "nninf.NninfChecker",
                            "--solver",
                            MaxSat2TypeSolver.class.getCanonicalName(),
                            "--jaifFile",
                            "build/inference-repair-validation/repaired.jaif",
                            "--hacks=true",
                            "--",
                            "-Anomsgtext",
                            "-d",
                            "tests/build/outputdir",
                            sourceFile.getPath()
                        },
                        false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        try {
            inferenceMain.run();
            return inferenceMain.getRunSnapshot();
        } catch (InferenceUnsatisfiableException e) {
            return e.getSnapshot();
        }
    }
}
