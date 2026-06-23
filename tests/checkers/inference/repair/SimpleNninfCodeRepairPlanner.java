package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;

/** Proposes source-code repairs for nninf diagnostics that annotation repair should not mask. */
public final class SimpleNninfCodeRepairPlanner {
    public List<CodeRepairCandidate> plan(List<RepairDiagnostic> diagnostics) {
        List<CodeRepairCandidate> candidates = new ArrayList<>();
        for (RepairDiagnostic diagnostic : diagnostics) {
            if (diagnostic.getKey().equals("dereference.of.nullable")) {
                candidates.add(
                        new CodeRepairCandidate(
                                diagnostic,
                                "replace nullable dereference with null-safe expression"));
            }
        }
        return candidates;
    }
}
