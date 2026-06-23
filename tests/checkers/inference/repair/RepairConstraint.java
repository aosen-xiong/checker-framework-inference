package checkers.inference.repair;

/** A labeled qualifier subtype constraint used for unsat-core-guided repair. */
public final class RepairConstraint {
    private final String label;
    private final RepairSlot subtype;
    private final RepairSlot supertype;
    private final RepairDiagnostic diagnostic;

    public RepairConstraint(
            String label,
            RepairSlot subtype,
            RepairSlot supertype,
            RepairDiagnostic diagnostic) {
        this.label = label;
        this.subtype = subtype;
        this.supertype = supertype;
        this.diagnostic = diagnostic;
    }

    public String getLabel() {
        return label;
    }

    public RepairSlot getSubtype() {
        return subtype;
    }

    public RepairSlot getSupertype() {
        return supertype;
    }

    public RepairDiagnostic getDiagnostic() {
        return diagnostic;
    }

    @Override
    public String toString() {
        return label + ": " + subtype + " <: " + supertype;
    }
}
