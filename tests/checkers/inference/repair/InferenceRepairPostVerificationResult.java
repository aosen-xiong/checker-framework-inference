package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;
import checkers.inference.test.InsertionResult;

import java.io.File;

/** Result of inserting inferred annotations after a repair and typechecking the inserted source. */
public final class InferenceRepairPostVerificationResult {
    private final File jaifFile;
    private final File annotatedSourceDirectory;
    private final File annotatedSourceFile;
    private final InsertionResult insertionResult;
    private final CheckerDiagnosticCapture.Result typecheckResult;

    public InferenceRepairPostVerificationResult(
            File jaifFile,
            File annotatedSourceDirectory,
            File annotatedSourceFile,
            InsertionResult insertionResult,
            CheckerDiagnosticCapture.Result typecheckResult) {
        this.jaifFile = jaifFile;
        this.annotatedSourceDirectory = annotatedSourceDirectory;
        this.annotatedSourceFile = annotatedSourceFile;
        this.insertionResult = insertionResult;
        this.typecheckResult = typecheckResult;
    }

    public File getJaifFile() {
        return jaifFile;
    }

    public File getAnnotatedSourceDirectory() {
        return annotatedSourceDirectory;
    }

    public File getAnnotatedSourceFile() {
        return annotatedSourceFile;
    }

    public InsertionResult getInsertionResult() {
        return insertionResult;
    }

    public CheckerDiagnosticCapture.Result getTypecheckResult() {
        return typecheckResult;
    }

    public boolean didInsertionFail() {
        return insertionResult == null || insertionResult.didFail();
    }

    public boolean didTypecheckFail() {
        return typecheckResult == null || typecheckResult.didTestFail();
    }

    public boolean isVerified() {
        return !didInsertionFail() && !didTypecheckFail();
    }
}
