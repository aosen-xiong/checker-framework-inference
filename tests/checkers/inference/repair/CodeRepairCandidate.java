package checkers.inference.repair;

/** A source-code edit proposed for diagnostics that are not annotation-only repairs. */
public final class CodeRepairCandidate {
    private final RepairDiagnostic diagnostic;
    private final String description;

    public CodeRepairCandidate(RepairDiagnostic diagnostic, String description) {
        this.diagnostic = diagnostic;
        this.description = description;
    }

    public RepairDiagnostic getDiagnostic() {
        return diagnostic;
    }

    public String getDescription() {
        return description;
    }
}
