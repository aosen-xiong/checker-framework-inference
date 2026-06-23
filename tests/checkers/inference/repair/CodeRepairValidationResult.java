package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;

import java.io.File;

/** Result of applying and checking one source-code repair candidate. */
public final class CodeRepairValidationResult {
    private final CodeRepairCandidate candidate;
    private final File repairedSourceFile;
    private final CheckerDiagnosticCapture.Result checkerResult;

    public CodeRepairValidationResult(
            CodeRepairCandidate candidate,
            File repairedSourceFile,
            CheckerDiagnosticCapture.Result checkerResult) {
        this.candidate = candidate;
        this.repairedSourceFile = repairedSourceFile;
        this.checkerResult = checkerResult;
    }

    public CodeRepairCandidate getCandidate() {
        return candidate;
    }

    public File getRepairedSourceFile() {
        return repairedSourceFile;
    }

    public CheckerDiagnosticCapture.Result getCheckerResult() {
        return checkerResult;
    }

    public boolean removesAllDiagnostics() {
        return checkerResult.getActualDiagnostics().isEmpty();
    }
}
