package checkers.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.checkerframework.framework.type.QualifierHierarchy;

import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;

/** Immutable summary of one inference solve attempt. */
public final class InferenceRunSnapshot {
    private final List<Slot> slots;
    private final List<Constraint> constraints;
    private final InferenceResult solverResult;
    private final QualifierHierarchy qualifierHierarchy;

    public InferenceRunSnapshot(
            Collection<Slot> slots,
            Collection<Constraint> constraints,
            InferenceResult solverResult) {
        this(slots, constraints, solverResult, null);
    }

    public InferenceRunSnapshot(
            Collection<Slot> slots,
            Collection<Constraint> constraints,
            InferenceResult solverResult,
            QualifierHierarchy qualifierHierarchy) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        this.constraints = Collections.unmodifiableList(new ArrayList<>(constraints));
        this.solverResult = solverResult;
        this.qualifierHierarchy = qualifierHierarchy;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public InferenceResult getSolverResult() {
        return solverResult;
    }

    public QualifierHierarchy getQualifierHierarchy() {
        return qualifierHierarchy;
    }

    public boolean hasSolution() {
        return solverResult != null && solverResult.hasSolution();
    }

    public Collection<Constraint> getUnsatisfiableConstraints() {
        if (solverResult == null || solverResult.hasSolution()) {
            return Collections.emptyList();
        }
        return solverResult.getUnsatisfiableConstraints();
    }
}
