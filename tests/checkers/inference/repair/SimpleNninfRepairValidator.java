package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    private File repairedSourceFile(RepairCandidate candidate) {
        File original = candidate.getTargetSlot().getSourceFile();
        return new File(outputDirectory, original.getName());
    }

    private void writeRepairedSource(RepairCandidate candidate, File repairedSourceFile) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create repair output directory: "
                            + outputDirectory.getAbsolutePath());
        }
        List<String> originalLines =
                InferenceTestUtilities.getLines(candidate.getTargetSlot().getSourceFile());
        List<String> repairedLines = new ArrayList<>();
        int targetLine = candidate.getTargetSlot().getLineNumber();
        for (int index = 0; index < originalLines.size(); index++) {
            String line = originalLines.get(index);
            if (line.trim().startsWith("// ::")) {
                continue;
            }
            if (index + 1 == targetLine) {
                line = insertQualifier(line, candidate.getQualifier());
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
