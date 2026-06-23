package checkers.inference.repair;

import static checkers.inference.repair.SimpleNninfUnsatCoreSolver.NONNULL;
import static checkers.inference.repair.SimpleNninfUnsatCoreSolver.NULLABLE;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Extracts fixture-shaped nninf repair constraints from checker diagnostics. */
public final class SimpleNninfConstraintExtractor {
    public List<RepairConstraint> extract(List<RepairDiagnostic> diagnostics) {
        List<RepairConstraint> constraints = new ArrayList<>();
        for (RepairDiagnostic diagnostic : diagnostics) {
            if (diagnostic.getKey().equals("assignment.type.incompatible")) {
                constraints.add(extractAssignmentConstraint(diagnostic));
            } else if (diagnostic.getKey().equals("argument.type.incompatible")) {
                constraints.add(extractMethodArgumentConstraint(diagnostic));
            }
        }
        return constraints;
    }

    private RepairConstraint extractAssignmentConstraint(RepairDiagnostic diagnostic) {
        File sourceFile = diagnostic.getSourceFile();
        List<String> lines = InferenceTestUtilities.getLines(sourceFile);
        int diagnosticLineNumber = toIntLine(diagnostic);
        String line = lines.get(diagnosticLineNumber - 1);
        String[] sides = line.replace(";", "").split("=");
        String lhsExpression = sides[0].trim();
        String lhsName = simpleName(lhsExpression);
        String rhsName = simpleName(sides[1]);

        RepairSlot rhs =
                findDeclarationSlot(sourceFile, lines, diagnosticLineNumber, rhsName, "assignment-rhs");
        RepairSlot lhs =
                lhsExpression.startsWith("this.")
                        ? findFieldSlot(sourceFile, lines, diagnosticLineNumber, lhsName, "assignment-lhs")
                        : findDeclarationSlot(
                                sourceFile, lines, diagnosticLineNumber, lhsName, "assignment-lhs");
        return new RepairConstraint(
                sourceFile.getName()
                        + ":"
                        + diagnosticLineNumber
                        + ":assignment.type.incompatible:rhs_to_lhs",
                rhs,
                lhs,
                diagnostic);
    }

    private RepairConstraint extractMethodArgumentConstraint(RepairDiagnostic diagnostic) {
        File sourceFile = diagnostic.getSourceFile();
        List<String> lines = InferenceTestUtilities.getLines(sourceFile);
        int diagnosticLineNumber = toIntLine(diagnostic);
        String line = lines.get(diagnosticLineNumber - 1).trim();
        String methodName = line.substring(0, line.indexOf('(')).trim();
        String argName = simpleName(line.substring(line.indexOf('(') + 1, line.indexOf(')')));

        RepairSlot argument =
                findDeclarationSlot(sourceFile, lines, diagnosticLineNumber, argName, "argument");
        RepairSlot formal =
                findMethodFormalSlot(sourceFile, lines, diagnosticLineNumber, methodName, "formal");
        return new RepairConstraint(
                sourceFile.getName()
                        + ":"
                        + diagnosticLineNumber
                        + ":argument.type.incompatible:arg_to_param",
                argument,
                formal,
                diagnostic);
    }

    private static RepairSlot findDeclarationSlot(
            File sourceFile, List<String> lines, int beforeLineNumber, String name, String role) {
        for (int index = beforeLineNumber - 2; index >= 0; index--) {
            String line = lines.get(index);
            if (declaresName(line, name)) {
                return new RepairSlot(
                        role + ":" + name,
                        sourceFile,
                        index + 1,
                        role,
                        qualifierForDeclaration(line));
            }
        }
        throw new IllegalArgumentException("Could not find declaration for " + name);
    }

    private static RepairSlot findFieldSlot(
            File sourceFile, List<String> lines, int beforeLineNumber, String name, String role) {
        for (int index = 0; index < beforeLineNumber - 1; index++) {
            String line = lines.get(index);
            if (declaresName(line, name) && line.contains(";") && !line.contains("(")) {
                return new RepairSlot(
                        role + ":" + name,
                        sourceFile,
                        index + 1,
                        role,
                        qualifierForDeclaration(line));
            }
        }
        throw new IllegalArgumentException("Could not find field declaration for " + name);
    }

    private static RepairSlot findMethodFormalSlot(
            File sourceFile,
            List<String> lines,
            int beforeLineNumber,
            String methodName,
            String role) {
        for (int index = 0; index < beforeLineNumber - 1; index++) {
            String line = lines.get(index);
            if (line.contains(methodName + "(") && line.contains(")")) {
                return new RepairSlot(
                        role + ":" + methodName + "#0",
                        sourceFile,
                        index + 1,
                        role,
                        qualifierForDeclaration(firstParameter(line)));
            }
        }
        throw new IllegalArgumentException("Could not find formal for " + methodName);
    }

    private static String firstParameter(String methodDeclaration) {
        return methodDeclaration.substring(
                methodDeclaration.indexOf('(') + 1, methodDeclaration.indexOf(')'));
    }

    private static boolean declaresName(String line, String name) {
        return line.matches(".*\\b" + name + "\\b.*")
                && (line.contains(";") || line.contains("(") || line.contains(","));
    }

    private static String qualifierForDeclaration(String declaration) {
        return declaration.contains("@Nullable") ? NULLABLE : NONNULL;
    }

    private static String simpleName(String expression) {
        String trimmed = expression.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex >= 0) {
            trimmed = trimmed.substring(dotIndex + 1);
        }
        return trimmed.replaceAll("[^A-Za-z0-9_].*$", "");
    }

    private static int toIntLine(RepairDiagnostic diagnostic) {
        long lineNumber = diagnostic.getLineNumber();
        if (lineNumber > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Line number too large: " + lineNumber);
        }
        return (int) lineNumber;
    }
}
