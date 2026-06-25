package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies simple source-code repairs to temporary files and checks them with nninf. */
public final class SimpleNninfCodeRepairValidator {
    private static final Pattern SIMPLE_IDENTIFIER =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern DECLARATION_PATTERN =
            Pattern.compile(
                    "(?:^|[,(;{])\\s*((?:@[A-Za-z_$][A-Za-z0-9_$.]*\\s+)*"
                            + "[A-Za-z_$][A-Za-z0-9_$.<>?,\\[\\] ]*)\\s+%s\\b");

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
                repairedLines.addAll(guardedDereference(candidate, line));
            } else {
                repairedLines.add(line);
            }
        }
        InferenceTestUtilities.writeLines(repairedLines, repairedSourceFile);
    }

    private static List<String> guardedDereference(CodeRepairCandidate candidate, String line) {
        CheckerAstDiagnosticContext astContext =
                CheckerAstDiagnosticContext.from(candidate.getDiagnostic());
        Optional<NullableDereferenceAstContext> nullableDereference =
                astContext.nullableDereference();
        String receiver =
                nullableDereference.isPresent()
                        ? nullableDereference.get().getReceiverSource()
                        : nullableReceiver(line);
        String indent = line.substring(0, line.indexOf(line.trim()));
        String trimmed = line.trim();
        List<String> repairedLines = new ArrayList<>();
        if (trimmed.startsWith("return ") && trimmed.endsWith(".length();")) {
            repairedLines.add(
                    indent + "return java.util.Objects.toString(" + receiver + ", \"\").length();");
            return repairedLines;
        }
        if (trimmed.endsWith(";")) {
            Optional<NullableFieldAstContext> nullableField =
                    nullableDereference.isPresent()
                            ? astContext.nullableFieldForReceiver(nullableDereference.get())
                            : Optional.empty();
            if (nullableField.isPresent()) {
                String fieldType = nullableField.get().getDeclaredTypeSource();
                repairedLines.add(indent + "if (this." + receiver + " == null) {");
                repairedLines.add(
                        indent
                                + "    throw new IllegalStateException(\""
                                + receiver
                                + " is null\");");
                repairedLines.add(indent + "}");
                repairedLines.add(indent + "@SuppressWarnings(\"cast.unsafe\")");
                repairedLines.add(
                        indent
                                + fieldType
                                + " "
                                + receiver
                                + " = (@nninf.qual.NonNull "
                                + fieldType
                                + ") this."
                                + receiver
                                + ";");
                repairedLines.add(line);
                return repairedLines;
            }
            repairedLines.add(indent + "if (" + receiver + " == null) {");
            repairedLines.add(
                    indent
                            + "    throw new IllegalStateException(\""
                            + receiver
                            + " is null\");");
            repairedLines.add(indent + "}");
            Optional<String> declaredType = declaredType(candidate, receiver);
            if (declaredType.isPresent() && SIMPLE_IDENTIFIER.matcher(receiver).matches()) {
                String nonNullReceiver = receiver + "NonNull";
                repairedLines.add(indent + "@SuppressWarnings(\"cast.unsafe\")");
                repairedLines.add(
                        indent
                                + "@nninf.qual.NonNull "
                                + declaredType.get()
                                + " "
                                + nonNullReceiver
                                + " = (@nninf.qual.NonNull "
                                + declaredType.get()
                                + ") "
                                + receiver
                                + ";");
                repairedLines.add(line.replace(receiver, nonNullReceiver));
            } else {
                repairedLines.add(line);
            }
            return repairedLines;
        }
        throw new IllegalArgumentException("Unsupported dereference repair line: " + line);
    }

    private static Optional<String> declaredType(CodeRepairCandidate candidate, String variableName) {
        List<String> lines = InferenceTestUtilities.getLines(candidate.getDiagnostic().getSourceFile());
        int diagnosticLine = (int) candidate.getDiagnostic().getLineNumber();
        Pattern pattern = Pattern.compile(String.format(DECLARATION_PATTERN.pattern(), variableName));
        for (int index = Math.min(diagnosticLine - 1, lines.size() - 1); index >= 0; index--) {
            Matcher matcher = pattern.matcher(lines.get(index));
            if (matcher.find()) {
                return Optional.of(
                        matcher.group(1)
                                .replaceAll("@[A-Za-z_$][A-Za-z0-9_$.]*\\s+", "")
                                .trim());
            }
        }
        return Optional.empty();
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
