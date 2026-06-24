package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InsertionResult;

import org.checkerframework.framework.util.ExecUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Reuses AFU insertion and checker typechecking after a repaired inference run succeeds. */
public final class InferenceRepairPostVerifier {
    private static final List<String> NNINF_TYPECHECK_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

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
                                nninf.NninfChecker.class,
                                Collections.singletonList(annotatedSourceFile),
                                NNINF_TYPECHECK_OPTIONS);
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
