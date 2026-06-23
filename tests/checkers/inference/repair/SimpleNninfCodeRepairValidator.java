package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Applies simple source-code repairs to temporary files and checks them with nninf. */
public final class SimpleNninfCodeRepairValidator {
    private final Class<?> checker;
    private final List<String> javacOptions;
    private final File outputDirectory;

    public SimpleNninfCodeRepairValidator(
            Class<?> checker, List<String> javacOptions, File outputDirectory) {
        this.checker = checker;
        this.javacOptions = new ArrayList<>(javacOptions);
        this.outputDirectory = outputDirectory;
    }

    public CodeRepairValidationResult validate(CodeRepairCandidate candidate) {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create code repair output directory: "
                            + outputDirectory.getAbsolutePath());
        }

        File repairedSourceFile =
                new File(outputDirectory, candidate.getDiagnostic().getSourceFile().getName());
        writeGuardedSource(candidate, repairedSourceFile);
        CheckerDiagnosticCapture.Result checkerResult =
                CheckerDiagnosticCapture.run(checker, repairedSourceFile, javacOptions);
        return new CodeRepairValidationResult(candidate, repairedSourceFile, checkerResult);
    }

    private static void writeGuardedSource(
            CodeRepairCandidate candidate, File repairedSourceFile) {
        List<String> originalLines =
                InferenceTestUtilities.getLines(candidate.getDiagnostic().getSourceFile());
        List<String> repairedLines = new ArrayList<>();
        int diagnosticLine = (int) candidate.getDiagnostic().getLineNumber();
        for (int index = 0; index < originalLines.size(); index++) {
            String line = originalLines.get(index);
            if (line.trim().startsWith("// ::")) {
                continue;
            }
            if (index + 1 == diagnosticLine) {
                repairedLines.add(guardedDereference(line));
            } else {
                repairedLines.add(line);
            }
        }
        InferenceTestUtilities.writeLines(repairedLines, repairedSourceFile);
    }

    private static String guardedDereference(String line) {
        String receiver = nullableReceiver(line);
        String indent = line.substring(0, line.indexOf(line.trim()));
        String trimmed = line.trim();
        if (trimmed.startsWith("return ") && trimmed.endsWith(".length();")) {
            return indent + "return java.util.Objects.toString(" + receiver + ", \"\").length();";
        }
        throw new IllegalArgumentException("Unsupported dereference repair line: " + line);
    }

    private static String nullableReceiver(String line) {
        String trimmed = line.trim();
        int dotIndex = trimmed.indexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("No dereference found: " + line);
        }
        String beforeDereference = trimmed.substring(0, dotIndex);
        int spaceIndex = beforeDereference.lastIndexOf(' ');
        return spaceIndex < 0 ? beforeDereference : beforeDereference.substring(spaceIndex + 1);
    }
}
