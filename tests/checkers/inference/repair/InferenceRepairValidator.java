package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Applies inference-guided repair candidates to temporary source copies and reruns inference. */
public final class InferenceRepairValidator {
    private final InferenceRepairConfiguration configuration;
    private final File originalSourceFile;
    private final File outputDirectory;
    private final InferenceRepairTargetExtractor targetExtractor = new InferenceRepairTargetExtractor();
    private final InferenceRepairEditProvider editProvider;
    private final InferenceRepairRunner runner;

    public InferenceRepairValidator(
            InferenceRepairConfiguration configuration,
            File originalSourceFile,
            File outputDirectory) {
        this(configuration, originalSourceFile, outputDirectory, new InferenceRepairEditGenerator());
    }

    public InferenceRepairValidator(
            InferenceRepairConfiguration configuration,
            File originalSourceFile,
            File outputDirectory,
            InferenceRepairEditProvider editProvider) {
        this(
                configuration,
                originalSourceFile,
                outputDirectory,
                editProvider,
                new DefaultInferenceRepairRunner(configuration));
    }

    public InferenceRepairValidator(
            InferenceRepairConfiguration configuration,
            File originalSourceFile,
            File outputDirectory,
            InferenceRepairEditProvider editProvider,
            InferenceRepairRunner runner) {
        this.configuration = configuration;
        this.originalSourceFile = originalSourceFile;
        this.outputDirectory = outputDirectory;
        this.editProvider = editProvider;
        this.runner = runner;
    }

    public InferenceRepairValidationResult validate(InferenceRepairCandidate candidate) {
        List<InferenceRepairAttempt> attempts = new ArrayList<>();
        InferenceRepairTarget target = targetExtractor.extract(originalSourceFile, candidate);
        String originalSource = readSource(originalSourceFile);
        int editAttempts = 0;
        for (InferenceRepairEdit edit : editProvider.generate(candidate, target, originalSource)) {
            if (editAttempts >= configuration.getMaxEditsPerCandidate()) {
                break;
            }
            editAttempts++;
            File attemptDirectory = attemptDirectory(candidate, edit);
            File repairedSourceFile = repairedSourceFile(attemptDirectory);
            File jaifFile = new File(attemptDirectory, "repaired.jaif");
            writeRepairedSource(repairedSourceFile, edit, target, originalSource);
            InferenceRepairRunResult runResult =
                    runner.run(
                            repairedSourceFile,
                            jaifFile,
                            new File(attemptDirectory, "annotated-source"));
            InferenceRepairAttempt attempt =
                    new InferenceRepairAttempt(
                            edit.getRepairKind(),
                            target,
                            repairedSourceFile,
                            edit.getDescription(),
                            edit.getReplacementSource(),
                            edit.getOrigin(),
                            runResult.getSnapshot(),
                            runResult.getPostVerificationResult());
            attempts.add(attempt);
            if (attempt.isFullyVerified()) {
                break;
            }
        }
        return new InferenceRepairValidationResult(candidate, attempts);
    }

    public InferenceRepairSearchResult validateAll(List<InferenceRepairCandidate> candidates) {
        List<InferenceRepairValidationResult> validationResults = new ArrayList<>();
        for (InferenceRepairCandidate candidate : candidates) {
            if (validationResults.size() >= configuration.getMaxCandidatesToValidate()) {
                break;
            }
            InferenceRepairValidationResult validationResult;
            try {
                validationResult = validate(candidate);
            } catch (IllegalArgumentException e) {
                validationResult = InferenceRepairValidationResult.failed(candidate, e);
            }
            validationResults.add(validationResult);
            if (validationResult.isFullyVerified()) {
                break;
            }
        }
        return new InferenceRepairSearchResult(validationResults);
    }

    private File attemptDirectory(InferenceRepairCandidate candidate, InferenceRepairEdit edit) {
        return new File(
                outputDirectory,
                "slot_"
                        + candidate.getTargetSlot().getId()
                        + File.separator
                        + edit.getDirectoryName());
    }

    private File repairedSourceFile(File attemptDirectory) {
        return new File(attemptDirectory, originalSourceFile.getName());
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

}
