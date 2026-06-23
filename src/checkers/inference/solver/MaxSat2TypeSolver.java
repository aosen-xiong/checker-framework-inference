package checkers.inference.solver;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.inference.DefaultInferenceResult;
import checkers.inference.InferenceResult;
import checkers.inference.InferenceSolver;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.serialization.CnfVecIntSerializer;

/**
 * This solver is used to convert any constraint set using a type system with only 2 types
 * (Top/Bottom), into a SAT problem. This SAT problem is then solved by SAT4J and the output is
 * converted back into an InferenceResult.
 */
public class MaxSat2TypeSolver implements InferenceSolver {

    // private QualifierHierarchy qualHierarchy;
    private Collection<Constraint> constraints;
    // private Collection<Slot> slots;

    // private AnnotationMirror defaultValue;
    private AnnotationMirror top;
    private AnnotationMirror bottom;
    private CnfVecIntSerializer serializer;
    private int baseSlotCount;

    @Override
    public InferenceResult solve(
            Map<String, String> configuration,
            Collection<Slot> slots,
            Collection<Constraint> constraints,
            QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment) {

        // this.slots = slots;
        this.constraints = constraints;
        // this.qualHierarchy = qualHierarchy;

        this.top = qualHierarchy.getTopAnnotations().iterator().next();
        this.bottom = qualHierarchy.getBottomAnnotations().iterator().next();
        this.baseSlotCount = maxSlotId(slots);
        this.serializer =
                new CnfVecIntSerializer(baseSlotCount) {
                    @Override
                    protected boolean isTop(ConstantSlot constantSlot) {
                        return AnnotationUtils.areSame(constantSlot.getValue(), top);
                    }
                };
        // TODO: This needs to be parameterized based on the type system
        // this.defaultValue = top;

        return solve();
    }

    public InferenceResult solve() {

        final EncodedConstraints encodedConstraints = encodeConstraints(constraints);
        final List<VecInt> hardClauses = encodedConstraints.hardClauses;
        final List<VecInt> softClauses = encodedConstraints.softClauses;

        // nextId describes the LARGEST id that might be found in a variable
        // if an exception occurs while creating a variable the id might be incremented
        // but the slot might not actually be recorded.  Therefore, nextId is NOT
        // the number of slots but the maximum you might encounter.
        // TODO: this is a workaround as currently when serialize existential constraint we lost the
        // real existential
        // TODO: variable id and create "fake" id stored in existentialToPotentialVar map.
        // TODO: thus here the value of totalVars is the real slots number stored in slotManager,
        // and plus the
        // TODO: "fake" slots number stored in existentialToPotentialVar
        final int totalVars =
                baseSlotCount + serializer.getExistentialToPotentialVar().size();
        final int totalClauses = hardClauses.size() + softClauses.size();

        // When .newBoth is called, SAT4J will run two solvers and return the result of the first to
        // halt
        final WeightedMaxSatDecorator solver =
                new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newBoth());

        solver.newVar(totalVars);
        solver.setExpectedNumberOfClauses(totalClauses);

        // arbitrary timeout selected for no particular reason
        solver.setTimeoutMs(1000000);

        VecInt lastClause = null;
        try {
            for (VecInt clause : hardClauses) {
                lastClause = clause;
                solver.addHardClause(clause);
            }
            for (VecInt clause : softClauses) {

                lastClause = clause;
                solver.addSoftClause(clause);
            }

        } catch (ContradictionException ce) {
            // This happens when adding a clause causes trivial contradiction, such as adding -1 to
            // {1}
            System.out.println(
                    "Not solvable! Contradiction exception "
                            + "when adding clause: "
                            + lastClause
                            + ".");

            return new DefaultInferenceResult(
                    explainUnsatisfiableConstraints(
                            encodedConstraints.hardClauseConstraints, totalVars));
        }

        boolean isSatisfiable;
        try {
            // isSatisfiable() launches the solvers and waits until one of them finishes
            isSatisfiable = solver.isSatisfiable();

        } catch (TimeoutException te) {
            throw new RuntimeException("MAX-SAT solving timeout! ");
        }

        if (!isSatisfiable) {
            System.out.println("Not solvable!");
            return new DefaultInferenceResult(
                    explainUnsatisfiableConstraints(
                            encodedConstraints.hardClauseConstraints, totalVars));
        }

        int[] solution = solver.model();
        // The following code decodes VecInt solution to the slot-annotation mappings
        final Map<Integer, AnnotationMirror> decodedSolution = new HashMap<>();
        final Map<Integer, Integer> existentialToPotentialIds =
                serializer.getExistentialToPotentialVar();

        for (Integer var : solution) {
            Integer potential = existentialToPotentialIds.get(Math.abs(var));
            if (potential != null) {
                // Assume the 'solution' output by the solver is already sorted in the ascending
                // order
                // of their absolute values. So the existential variables come after the potential
                // variables,
                // which means the potential slot corresponding to the current existential variable
                // is
                // already inserted into 'solutions'
                assert decodedSolution.containsKey(potential);
                if (var < 0) {
                    // The existential variable is false, so the potential variable should not be
                    // inserted.
                    // Remove it from the solution.
                    decodedSolution.remove(potential);
                }
            } else {
                boolean isTop = var < 0;
                if (isTop) {
                    var = -var;
                }
                decodedSolution.put(var, isTop ? top : bottom);
            }
        }

        return new DefaultInferenceResult(decodedSolution);
    }

    private EncodedConstraints encodeConstraints(Collection<Constraint> constraints) {
        EncodedConstraints encodedConstraints = new EncodedConstraints();
        for (Constraint constraint : constraints) {
            for (VecInt clause : constraint.serialize(serializer)) {
                if (clause.size() == 0) {
                    continue;
                }
                if (constraint instanceof PreferenceConstraint) {
                    encodedConstraints.softClauses.add(clause);
                } else {
                    encodedConstraints.hardClauses.add(clause);
                    encodedConstraints.hardClauseConstraints.add(constraint);
                }
            }
        }
        return encodedConstraints;
    }

    private Collection<Constraint> explainUnsatisfiableConstraints(
            List<Constraint> hardClauseConstraints, int totalVars) {
        List<Constraint> coreConstraints = new ArrayList<>(hardClauseConstraints);

        int index = 0;
        while (index < coreConstraints.size()) {
            List<Constraint> candidate = new ArrayList<>(coreConstraints);
            candidate.remove(index);
            EncodedConstraints encodedCandidate = encodeConstraints(candidate);
            if (!isHardClauseSetSatisfiable(encodedCandidate.hardClauses, totalVars)) {
                coreConstraints = candidate;
            } else {
                index++;
            }
        }

        return new HashSet<>(coreConstraints);
    }

    private boolean isHardClauseSetSatisfiable(List<VecInt> clauses, int totalVars) {
        final WeightedMaxSatDecorator solver =
                new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newBoth());
        solver.newVar(totalVars);
        solver.setExpectedNumberOfClauses(clauses.size());
        solver.setTimeoutMs(1000000);

        try {
            for (VecInt clause : clauses) {
                solver.addHardClause(clause);
            }
            return solver.isSatisfiable();
        } catch (ContradictionException ce) {
            return false;
        } catch (TimeoutException te) {
            throw new RuntimeException("MAX-SAT solving timeout! ");
        }
    }

    private static final class EncodedConstraints {
        private final List<VecInt> hardClauses = new LinkedList<>();
        private final List<VecInt> softClauses = new LinkedList<>();
        private final List<Constraint> hardClauseConstraints = new LinkedList<>();
    }

    private static int maxSlotId(Collection<Slot> slots) {
        int maxSlotId = 0;
        for (Slot slot : slots) {
            maxSlotId = Math.max(maxSlotId, slot.getId());
        }
        return maxSlotId;
    }
}
