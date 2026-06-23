package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;

import java.io.File;

/** Result of applying and checking one repair candidate on a temporary source copy. */
public final class RepairValidationResult {
    private final RepairCandidate candidate;
    private final File repairedSourceFile;
    private final CheckerDiagnosticCapture.Result checkerResult;

    public RepairValidationResult(
            RepairCandidate candidate,
            File repairedSourceFile,
            CheckerDiagnosticCapture.Result checkerResult) {
        this.candidate = candidate;
        this.repairedSourceFile = repairedSourceFile;
        this.checkerResult = checkerResult;
    }

    public RepairCandidate getCandidate() {
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
