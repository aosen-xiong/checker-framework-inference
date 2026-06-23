package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;

/** A tiny unsat-core solver for the nninf two-point nullness lattice. */
public final class SimpleNninfUnsatCoreSolver {
    static final String NONNULL = "@NonNull";
    static final String NULLABLE = "@Nullable";

    public UnsatCoreResult solve(List<RepairConstraint> constraints) {
        List<RepairConstraint> core = new ArrayList<>();
        for (RepairConstraint constraint : constraints) {
            if (!isSubtype(
                    constraint.getSubtype().getQualifier(),
                    constraint.getSupertype().getQualifier())) {
                core.add(constraint);
            }
        }
        return new UnsatCoreResult(core);
    }

    private static boolean isSubtype(String subtype, String supertype) {
        if (subtype.equals(supertype)) {
            return true;
        }
        return subtype.equals(NONNULL) && supertype.equals(NULLABLE);
    }
}
