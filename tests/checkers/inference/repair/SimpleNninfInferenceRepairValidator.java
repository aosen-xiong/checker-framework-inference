package checkers.inference.repair;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;
import checkers.inference.solver.MaxSat2TypeSolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Applies inference-guided repair candidates to temporary source copies and reruns inference. */
public final class SimpleNninfInferenceRepairValidator {
    private final File originalSourceFile;
    private final File outputDirectory;
    private final InferenceRepairTargetExtractor targetExtractor = new InferenceRepairTargetExtractor();

    public SimpleNninfInferenceRepairValidator(File originalSourceFile, File outputDirectory) {
        this.originalSourceFile = originalSourceFile;
        this.outputDirectory = outputDirectory;
    }

    public InferenceRepairValidationResult validate(InferenceRepairCandidate candidate) {
        List<InferenceRepairAttempt> attempts = new ArrayList<>();
        InferenceRepairTarget target = targetExtractor.extract(originalSourceFile, candidate);
        for (InferenceRepairKind repairKind : repairSearchOrder(candidate, target)) {
            File repairedSourceFile = repairedSourceFile(candidate, repairKind);
            String appliedEdit = writeRepairedSource(repairedSourceFile, repairKind, target);
            InferenceRunSnapshot snapshot = runInference(repairedSourceFile);
            InferenceRepairAttempt attempt =
                    new InferenceRepairAttempt(
                            repairKind, target, repairedSourceFile, appliedEdit, snapshot);
            attempts.add(attempt);
            if (attempt.solvesInference()) {
                break;
            }
        }
        return new InferenceRepairValidationResult(candidate, attempts);
    }

    public InferenceRepairSearchResult validateAll(List<InferenceRepairCandidate> candidates) {
        List<InferenceRepairValidationResult> validationResults = new ArrayList<>();
        for (InferenceRepairCandidate candidate : candidates) {
            InferenceRepairValidationResult validationResult;
            try {
                validationResult = validate(candidate);
            } catch (IllegalArgumentException e) {
                validationResult = InferenceRepairValidationResult.failed(candidate, e);
            }
            validationResults.add(validationResult);
            if (validationResult.solvesInference()) {
                break;
            }
        }
        return new InferenceRepairSearchResult(validationResults);
    }

    private File repairedSourceFile(
            InferenceRepairCandidate candidate, InferenceRepairKind repairKind) {
        return new File(
                new File(
                        outputDirectory,
                        "slot_"
                                + candidate.getTargetSlot().getId()
                                + File.separator
                                + repairKind.name().toLowerCase()),
                originalSourceFile.getName());
    }

    private List<InferenceRepairKind> repairSearchOrder(
            InferenceRepairCandidate candidate, InferenceRepairTarget target) {
        List<InferenceRepairKind> repairKinds = new ArrayList<>();
        if (supportsRepairKind(candidate.getRepairKind(), target)) {
            repairKinds.add(candidate.getRepairKind());
        }
        if (!repairKinds.contains(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK)) {
            repairKinds.add(InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK);
        }
        return repairKinds;
    }

    private static boolean supportsRepairKind(
            InferenceRepairKind repairKind, InferenceRepairTarget target) {
        return repairKind != InferenceRepairKind.INSERT_NULL_GUARD
                || "VARIABLE".equals(target.getTreeKind());
    }

    private String writeRepairedSource(
            File repairedSourceFile, InferenceRepairKind repairKind, InferenceRepairTarget target) {
        File parentDirectory = repairedSourceFile.getParentFile();
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + parentDirectory.getAbsolutePath());
        }

        String originalSource = readSource(originalSourceFile);
        String replacement = repairedTargetSource(originalSource, repairKind, target);
        String repairedSource =
                originalSource.substring(0, checkedOffset(target.getStartOffset()))
                        + replacement
                        + originalSource.substring(checkedOffset(target.getEndOffset()));
        writeSource(repairedSourceFile, repairedSource);
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

    private static String repairedTargetSource(
            String originalSource, InferenceRepairKind repairKind, InferenceRepairTarget target) {
        String targetSource =
                originalSource.substring(
                        checkedOffset(target.getStartOffset()), checkedOffset(target.getEndOffset()));
        if (repairKind == InferenceRepairKind.INSERT_NULL_GUARD) {
            return nullGuardFor(originalSource, target)
                    + "\n"
                    + indentationBefore(target, originalSource)
                    + targetSource;
        }
        if (repairKind == InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK) {
            return replaceRhsWithNonNullFallback(targetSource);
        }
        return weakenAnnotation(targetSource);
    }

    private static String nullGuardFor(String originalSource, InferenceRepairTarget target) {
        String targetSource =
                originalSource.substring(
                        checkedOffset(target.getStartOffset()), checkedOffset(target.getEndOffset()));
        String rhs = targetSource.substring(targetSource.indexOf('=') + 1).replace(";", "").trim();
        return "if (" + rhs + " == null) { return; }";
    }

    private static String replaceRhsWithNonNullFallback(String targetSource) {
        if (!targetSource.contains("=")) {
            return "\"\"";
        }
        return targetSource.substring(0, targetSource.indexOf('=') + 1) + " \"\";";
    }

    private static String weakenAnnotation(String targetSource) {
        return targetSource.replace("@NonNull String", "@Nullable String");
    }

    private static String indentationBefore(InferenceRepairTarget target, String source) {
        int start = checkedOffset(target.getStartOffset());
        int lineStart = source.lastIndexOf('\n', start - 1) + 1;
        return source.substring(lineStart, start);
    }

    private static int checkedOffset(long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid source offset: " + offset);
        }
        return (int) offset;
    }

    private static String readSource(File sourceFile) {
        try {
            return new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + sourceFile, e);
        }
    }

    private static void writeSource(File sourceFile, String source) {
        try {
            Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write source file: " + sourceFile, e);
        }
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
