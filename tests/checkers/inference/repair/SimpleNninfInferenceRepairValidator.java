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
    private final InferenceRepairEditGenerator editGenerator = new InferenceRepairEditGenerator();

    public SimpleNninfInferenceRepairValidator(File originalSourceFile, File outputDirectory) {
        this.originalSourceFile = originalSourceFile;
        this.outputDirectory = outputDirectory;
    }

    public InferenceRepairValidationResult validate(InferenceRepairCandidate candidate) {
        List<InferenceRepairAttempt> attempts = new ArrayList<>();
        InferenceRepairTarget target = targetExtractor.extract(originalSourceFile, candidate);
        String originalSource = readSource(originalSourceFile);
        for (InferenceRepairEdit edit : editGenerator.generate(candidate, target, originalSource)) {
            File repairedSourceFile = repairedSourceFile(candidate, edit);
            writeRepairedSource(repairedSourceFile, edit, target, originalSource);
            InferenceRunSnapshot snapshot = runInference(repairedSourceFile);
            InferenceRepairAttempt attempt =
                    new InferenceRepairAttempt(
                            edit.getRepairKind(),
                            target,
                            repairedSourceFile,
                            edit.getDescription(),
                            snapshot);
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

    private File repairedSourceFile(InferenceRepairCandidate candidate, InferenceRepairEdit edit) {
        return new File(
                new File(
                        outputDirectory,
                        "slot_"
                                + candidate.getTargetSlot().getId()
                                + File.separator
                                + edit.getDirectoryName()),
                originalSourceFile.getName());
    }

    private static void writeRepairedSource(
            File repairedSourceFile,
            InferenceRepairEdit edit,
            InferenceRepairTarget target,
            String originalSource) {
        File parentDirectory = repairedSourceFile.getParentFile();
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + parentDirectory.getAbsolutePath());
        }

        String repairedSource =
                originalSource.substring(0, checkedOffset(target.getStartOffset()))
                        + edit.getReplacementSource()
                        + originalSource.substring(checkedOffset(target.getEndOffset()));
        writeSource(repairedSourceFile, repairedSource);
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
