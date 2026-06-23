package checkers.inference.repair;

import java.util.List;

/** A repair strategy for one family of checker diagnostics. */
public interface RepairHandler {
    String getName();

    boolean supports(RepairDiagnostic diagnostic);

    RepairHandlerResult repair(List<RepairDiagnostic> diagnostics);
}
