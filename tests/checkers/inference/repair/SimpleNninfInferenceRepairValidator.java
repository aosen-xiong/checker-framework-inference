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
        List<InferenceRepairAttempt> attempts = new ArrayList<>();
        for (InferenceRepairKind repairKind : repairSearchOrder(candidate)) {
            File repairedSourceFile = repairedSourceFile(repairKind);
            String appliedEdit = writeRepairedSource(repairedSourceFile, repairKind);
            InferenceRunSnapshot snapshot = runInference(repairedSourceFile);
            InferenceRepairAttempt attempt =
                    new InferenceRepairAttempt(
                            repairKind, repairedSourceFile, appliedEdit, snapshot);
            attempts.add(attempt);
            if (attempt.solvesInference()) {
                break;
            }
        }
        return new InferenceRepairValidationResult(candidate, attempts);
    }

    private File repairedSourceFile(InferenceRepairKind repairKind) {
        return new File(
                new File(outputDirectory, repairKind.name().toLowerCase()),
                originalSourceFile.getName());
    }

    private List<InferenceRepairKind> repairSearchOrder(InferenceRepairCandidate candidate) {
        List<InferenceRepairKind> repairKinds = new ArrayList<>();
        repairKinds.add(candidate.getRepairKind());
        if (!repairKinds.contains(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK)) {
            repairKinds.add(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK);
        }
        return repairKinds;
    }

    private String writeRepairedSource(File repairedSourceFile, InferenceRepairKind repairKind) {
        File parentDirectory = repairedSourceFile.getParentFile();
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + parentDirectory.getAbsolutePath());
        }

        List<String> originalLines = InferenceTestUtilities.getLines(originalSourceFile);
        List<String> repairedLines = new ArrayList<>();
        boolean changed = false;
        for (String line : originalLines) {
            if (!changed && line.contains("@NonNull String") && line.contains("=")) {
                if (repairKind == InferenceRepairKind.INSERT_NULL_GUARD) {
                    repairedLines.add(nullGuardFor(line));
                } else if (repairKind == InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK) {
                    line = replaceRhsWithNonNullFallback(line);
                } else {
                    line = weakenAnnotation(line);
                }
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
        return editDescription(repairKind);
    }

    private static String editDescription(InferenceRepairKind repairKind) {
        if (repairKind == InferenceRepairKind.INSERT_NULL_GUARD) {
            return "insert null guard before @NonNull local assignment";
        }
        if (repairKind == InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK) {
            return "replace nullable RHS with non-null fallback literal";
        }
        return "weaken @NonNull annotation to @Nullable";
    }

    private static String nullGuardFor(String line) {
        String indentation = line.substring(0, line.indexOf(line.trim()));
        String rhs = line.substring(line.indexOf('=') + 1).replace(";", "").trim();
        return indentation + "if (" + rhs + " == null) { return; }";
    }

    private static String replaceRhsWithNonNullFallback(String line) {
        return line.substring(0, line.indexOf('=') + 1) + " \"\";";
    }

    private static String weakenAnnotation(String line) {
        return line.replace("@NonNull String", "@Nullable String");
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
