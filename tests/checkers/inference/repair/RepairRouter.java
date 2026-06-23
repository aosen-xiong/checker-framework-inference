package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;

/** Routes diagnostics to repair handlers. */
public final class RepairRouter {
    private final List<RepairHandler> handlers;

    public RepairRouter(List<RepairHandler> handlers) {
        this.handlers = new ArrayList<>(handlers);
    }

    public List<RepairHandlerResult> repair(List<RepairDiagnostic> diagnostics) {
        List<RepairHandlerResult> results = new ArrayList<>();
        for (RepairHandler handler : handlers) {
            List<RepairDiagnostic> handlerDiagnostics = new ArrayList<>();
            for (RepairDiagnostic diagnostic : diagnostics) {
                if (handler.supports(diagnostic)) {
                    handlerDiagnostics.add(diagnostic);
                }
            }
            if (!handlerDiagnostics.isEmpty()) {
                results.add(handler.repair(handlerDiagnostics));
            }
        }
        return results;
    }
}
