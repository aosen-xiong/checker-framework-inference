package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Applies candidate repairs to temporary files and checks them with nninf. */
public final class SimpleNninfRepairValidator {
    private final Class<?> checker;
    private final List<String> javacOptions;
    private final File outputDirectory;

    public SimpleNninfRepairValidator(
            Class<?> checker, List<String> javacOptions, File outputDirectory) {
        this.checker = checker;
        this.javacOptions = new ArrayList<>(javacOptions);
        this.outputDirectory = outputDirectory;
    }

    public RepairValidationResult validate(RepairCandidate candidate) {
        File repairedSourceFile = repairedSourceFile(candidate);
        writeRepairedSource(candidate, repairedSourceFile);
        CheckerDiagnosticCapture.Result checkerResult =
                CheckerDiagnosticCapture.run(checker, repairedSourceFile, javacOptions);
        return new RepairValidationResult(candidate, repairedSourceFile, checkerResult);
    }

    public RepairBatchValidationResult validateAll(List<RepairCandidate> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one candidate is required.");
        }
        File sourceFile = candidates.get(0).getTargetSlot().getSourceFile();
        for (RepairCandidate candidate : candidates) {
            if (!candidate.getTargetSlot().getSourceFile().equals(sourceFile)) {
                throw new IllegalArgumentException("All candidates must target the same source file.");
            }
        }
        File repairedSourceFile = new File(outputDirectory, sourceFile.getName());
        writeRepairedSource(candidates, repairedSourceFile);
        CheckerDiagnosticCapture.Result checkerResult =
                CheckerDiagnosticCapture.run(checker, repairedSourceFile, javacOptions);
        return new RepairBatchValidationResult(candidates, repairedSourceFile, checkerResult);
    }

    private File repairedSourceFile(RepairCandidate candidate) {
        File original = candidate.getTargetSlot().getSourceFile();
        return new File(outputDirectory, original.getName());
    }

    private void writeRepairedSource(RepairCandidate candidate, File repairedSourceFile) {
        writeRepairedSource(java.util.Collections.singletonList(candidate), repairedSourceFile);
    }

    private void writeRepairedSource(List<RepairCandidate> candidates, File repairedSourceFile) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + outputDirectory.getAbsolutePath());
        }
        List<String> originalLines =
                InferenceTestUtilities.getLines(candidates.get(0).getTargetSlot().getSourceFile());
        Map<Integer, List<RepairCandidate>> candidatesByLine = new HashMap<>();
        for (RepairCandidate candidate : candidates) {
            int lineNumber = candidate.getTargetSlot().getLineNumber();
            candidatesByLine.computeIfAbsent(lineNumber, key -> new ArrayList<>()).add(candidate);
        }
        List<String> repairedLines = new ArrayList<>();
        for (int index = 0; index < originalLines.size(); index++) {
            String line = originalLines.get(index);
            if (line.trim().startsWith("// ::")) {
                continue;
            }
            List<RepairCandidate> lineCandidates = candidatesByLine.get(index + 1);
            if (lineCandidates != null) {
                for (RepairCandidate candidate : lineCandidates) {
                    line = insertQualifier(line, candidate.getQualifier());
                }
            }
            repairedLines.add(line);
        }
        InferenceTestUtilities.writeLines(repairedLines, repairedSourceFile);
    }

    private static String insertQualifier(String line, String qualifier) {
        if (line.contains(qualifier)) {
            return line;
        }
        return line.replaceFirst("\\bString\\b", qualifier + " String");
    }
}
