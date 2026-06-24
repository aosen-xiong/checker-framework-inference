package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InsertionResult;

import org.checkerframework.framework.util.ExecUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;

/** Reuses AFU insertion and checker typechecking after a repaired inference run succeeds. */
public final class InferenceRepairPostVerifier {
    private final InferenceRepairConfiguration configuration;

    public InferenceRepairPostVerifier(InferenceRepairConfiguration configuration) {
        this.configuration = configuration;
    }

    public InferenceRepairPostVerificationResult verify(
            File repairedSourceFile, File jaifFile, File annotatedSourceDirectory) {
        InsertionResult insertionResult =
                insertAnnotations(repairedSourceFile, jaifFile, annotatedSourceDirectory);
        File annotatedSourceFile =
                insertionResult.didFail()
                        ? null
                        : findAnnotatedSourceFile(
                                annotatedSourceDirectory, repairedSourceFile.getName());
        CheckerDiagnosticCapture.Result typecheckResult =
                annotatedSourceFile == null
                        ? null
                        : CheckerDiagnosticCapture.run(
                                configuration.getChecker(),
                                Collections.singletonList(annotatedSourceFile),
                                configuration.getTypecheckJavacOptions());
        return new InferenceRepairPostVerificationResult(
                jaifFile,
                annotatedSourceDirectory,
                annotatedSourceFile,
                insertionResult,
                typecheckResult);
    }

    private static InsertionResult insertAnnotations(
            File repairedSourceFile, File jaifFile, File annotatedSourceDirectory) {
        ensureDirectoryExists(annotatedSourceDirectory);
        String pathToAfuScripts = System.getProperty("path.afu.scripts", "");
        String insertAnnotationsScript =
                pathToAfuScripts.isEmpty()
                        ? "insert-annotations-to-source"
                        : pathToAfuScripts + File.separator + "insert-annotations-to-source";
        String[] options =
                new String[] {
                    insertAnnotationsScript,
                    "-v",
                    "-d",
                    annotatedSourceDirectory.getAbsolutePath(),
                    jaifFile.getAbsolutePath(),
                    repairedSourceFile.getAbsolutePath()
                };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int returnCode = ExecUtil.execute(options, outputStream, outputStream);
        return new InsertionResult(options, returnCode != 0, outputStream.toString());
    }

    private static File findAnnotatedSourceFile(File directory, String fileName) {
        File[] children = directory.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                File nested = findAnnotatedSourceFile(child, fileName);
                if (nested != null) {
                    return nested;
                }
            } else if (fileName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    private static void ensureDirectoryExists(File path) {
        if (!path.exists() && !path.mkdirs()) {
            throw new RuntimeException("Could not make directory: " + path.getAbsolutePath());
        }
    }
}
