package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of solving labeled repair constraints. */
public final class UnsatCoreResult {
    private final List<RepairConstraint> unsatCore;

    public UnsatCoreResult(List<RepairConstraint> unsatCore) {
        this.unsatCore = Collections.unmodifiableList(new ArrayList<>(unsatCore));
    }

    public boolean isSatisfiable() {
        return unsatCore.isEmpty();
    }

    public List<RepairConstraint> getUnsatCore() {
        return unsatCore;
    }
}
