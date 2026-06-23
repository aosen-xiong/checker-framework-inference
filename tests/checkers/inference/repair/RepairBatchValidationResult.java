package checkers.inference.repair;

import checkers.inference.test.CheckerDiagnosticCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of applying and checking a batch of repair candidates on one temporary source copy. */
public final class RepairBatchValidationResult {
    private final List<RepairCandidate> candidates;
    private final File repairedSourceFile;
    private final CheckerDiagnosticCapture.Result checkerResult;

    public RepairBatchValidationResult(
            List<RepairCandidate> candidates,
            File repairedSourceFile,
            CheckerDiagnosticCapture.Result checkerResult) {
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
        this.repairedSourceFile = repairedSourceFile;
        this.checkerResult = checkerResult;
    }

    public List<RepairCandidate> getCandidates() {
        return candidates;
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
