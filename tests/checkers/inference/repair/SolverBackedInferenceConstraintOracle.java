package checkers.inference.repair;

import org.checkerframework.framework.type.QualifierHierarchy;

import checkers.inference.InferenceResult;
import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;
import checkers.inference.solver.MaxSat2TypeSolver;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Satisfiability oracle that reruns MaxSat2 over retained real CFI constraints. */
public final class SolverBackedInferenceConstraintOracle
        implements InferenceConstraintSatisfiabilityOracle {
    private final Collection<Slot> slots;
    private final QualifierHierarchy qualifierHierarchy;
    private final Map<String, Constraint> constraintsByContextKey = new LinkedHashMap<>();

    public SolverBackedInferenceConstraintOracle(
            Collection<Slot> slots,
            Collection<Constraint> constraints,
            QualifierHierarchy qualifierHierarchy) {
        this.slots = new ArrayList<>(slots);
        this.qualifierHierarchy = qualifierHierarchy;
        for (Constraint constraint : constraints) {
            constraintsByContextKey.put(
                    InferenceConstraintContextFormatter.format(constraint).summarize(),
                    constraint);
        }
    }

    @Override
    public boolean isSatisfiable(List<InferenceConstraintContext> retainedContexts) {
        if (qualifierHierarchy == null) {
            throw new IllegalStateException("A qualifier hierarchy is required for solver-backed MCS.");
        }
        List<Constraint> retainedConstraints = new ArrayList<>();
        for (InferenceConstraintContext context : retainedContexts) {
            Constraint constraint = constraintsByContextKey.get(context.summarize());
            if (constraint != null) {
                retainedConstraints.add(constraint);
            }
        }
        InferenceResult result = solveQuietly(retainedConstraints);
        return result.hasSolution();
    }

    private InferenceResult solveQuietly(List<Constraint> retainedConstraints) {
        PrintStream originalOut = System.out;
        try {
            System.setOut(
                    new PrintStream(
                            new OutputStream() {
                                @Override
                                public void write(int b) {}
                            }));
            return new MaxSat2TypeSolver()
                    .solve(
                            Collections.<String, String>emptyMap(),
                            slots,
                            retainedConstraints,
                            qualifierHierarchy,
                            null);
        } finally {
            System.setOut(originalOut);
        }
    }
}
