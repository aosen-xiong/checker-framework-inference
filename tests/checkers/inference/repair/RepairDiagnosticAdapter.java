package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** Converts Checker Framework diagnostics into repair diagnostics. */
public final class RepairDiagnosticAdapter {
    private RepairDiagnosticAdapter() {}

    public static List<RepairDiagnostic> fromCaptureResult(CheckerDiagnosticCapture.Result result) {
        List<RepairDiagnostic> repairDiagnostics = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : result.getActualDiagnostics()) {
            String sourceName = diagnostic.getSource().getName();
            repairDiagnostics.add(
                    new RepairDiagnostic(
                            result.getChecker().getCanonicalName(),
                            resolveSourceFile(result.getSourceFiles(), sourceName),
                            diagnostic.getLineNumber(),
                            diagnostic.getKind(),
                            extractDiagnosticKey(diagnostic.getMessage(null)),
                            diagnostic.getMessage(null)));
        }
        return repairDiagnostics;
    }

    private static File resolveSourceFile(List<File> sourceFiles, String diagnosticSourceName) {
        for (File sourceFile : sourceFiles) {
            if (sourceFile.getPath().equals(diagnosticSourceName)
                    || sourceFile.getAbsolutePath().equals(diagnosticSourceName)
                    || sourceFile.getName().equals(new File(diagnosticSourceName).getName())) {
                return sourceFile;
            }
        }
        return new File(diagnosticSourceName);
    }

    private static String extractDiagnosticKey(String message) {
        if (message.startsWith("(") && message.endsWith(")")) {
            return message.substring(1, message.length() - 1);
        }
        return message;
    }
}
