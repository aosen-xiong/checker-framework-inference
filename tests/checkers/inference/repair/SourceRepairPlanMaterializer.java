package checkers.inference.repair;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Materializes supported source repair plans into temporary Java source files. */
public final class SourceRepairPlanMaterializer {
    private static final Pattern METHOD_NAME_PATTERN =
            Pattern.compile("\\.([A-Za-z_$][A-Za-z0-9_$]*)\\(");
    private static final Pattern METHOD_PARAMETER_PATTERN =
            Pattern.compile("Method\\.parameter\\s+(\\d+)");

    public SourceRepairPlanMaterialization materialize(
            SourceRepairPlan plan, File originalSourceFile, File outputDirectory) {
        if (plan.getSteps().size() != 1) {
            return SourceRepairPlanMaterialization.failed("Only one-step plans are supported.");
        }
        SourceRepairPlanStep step = plan.getSteps().get(0);
        if (!"InsertQualifier".equals(step.getEditKind())) {
            return SourceRepairPlanMaterialization.failed(
                    "Unsupported edit kind: " + step.getEditKind());
        }
        try {
            String source = read(originalSourceFile);
            String repaired = insertQualifier(source, originalSourceFile, step);
            File repairedSourceFile = new File(outputDirectory, originalSourceFile.getName());
            write(repairedSourceFile, repaired);
            return SourceRepairPlanMaterialization.materialized(repairedSourceFile);
        } catch (RuntimeException e) {
            return SourceRepairPlanMaterialization.failed(e.toString());
        }
    }

    private static String insertQualifier(
            String source, File sourceFile, SourceRepairPlanStep step) {
        ParameterTarget target = parameterTarget(sourceFile, step.getRepairUnitId());
        String insertion = step.getQualifier() + " ";
        return source.substring(0, target.typeStart)
                + insertion
                + source.substring(target.typeStart);
    }

    private static ParameterTarget parameterTarget(File sourceFile, String repairUnitId) {
        String methodName = methodName(repairUnitId);
        int parameterIndex = parameterIndex(repairUnitId);
        ParsedUnit parsedUnit = parse(sourceFile);
        ParameterFinder finder = new ParameterFinder(methodName, parameterIndex);
        finder.scan(parsedUnit.compilationUnit, null);
        VariableTree parameter = finder.getFoundParameter();
        if (parameter == null) {
            throw new IllegalArgumentException(
                    "Could not resolve parameter repair unit: " + repairUnitId);
        }
        long typeStart =
                parsedUnit.sourcePositions.getStartPosition(
                        parsedUnit.compilationUnit, parameter.getType());
        if (typeStart < 0 || typeStart > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid parameter type position: " + typeStart);
        }
        return new ParameterTarget((int) typeStart);
    }

    private static String methodName(String repairUnitId) {
        Matcher matcher = METHOD_NAME_PATTERN.matcher(repairUnitId);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No method name in repair unit id: " + repairUnitId);
        }
        return matcher.group(1);
    }

    private static int parameterIndex(String repairUnitId) {
        Matcher matcher = METHOD_PARAMETER_PATTERN.matcher(repairUnitId);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "No method parameter index in repair unit id: " + repairUnitId);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static ParsedUnit parse(File sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required to parse repair plans.");
        }
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(null, null, null)) {
            JavacTask task =
                    (JavacTask)
                            compiler.getTask(
                                    null,
                                    fileManager,
                                    null,
                                    null,
                                    null,
                                    fileManager.getJavaFileObjects(sourceFile));
            CompilationUnitTree compilationUnit = task.parse().iterator().next();
            return new ParsedUnit(compilationUnit, Trees.instance(task).getSourcePositions());
        } catch (IOException e) {
            throw new RuntimeException("Could not parse " + sourceFile, e);
        }
    }

    private static String read(File sourceFile) {
        try {
            return new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + sourceFile, e);
        }
    }

    private static void write(File sourceFile, String source) {
        File parent = sourceFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        try {
            Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write source file: " + sourceFile, e);
        }
    }

    private static final class ParameterFinder extends TreeScanner<Void, Void> {
        private final String methodName;
        private final int parameterIndex;
        private VariableTree foundParameter;

        private ParameterFinder(String methodName, int parameterIndex) {
            this.methodName = methodName;
            this.parameterIndex = parameterIndex;
        }

        @Override
        public Void visitMethod(MethodTree methodTree, Void unused) {
            if (foundParameter != null) {
                return null;
            }
            if (!methodTree.getName().contentEquals(methodName)) {
                return super.visitMethod(methodTree, unused);
            }
            List<? extends VariableTree> parameters = methodTree.getParameters();
            if (parameterIndex < parameters.size()) {
                foundParameter = parameters.get(parameterIndex);
            }
            return null;
        }

        private VariableTree getFoundParameter() {
            return foundParameter;
        }
    }

    private static final class ParameterTarget {
        private final int typeStart;

        private ParameterTarget(int typeStart) {
            this.typeStart = typeStart;
        }
    }

    private static final class ParsedUnit {
        private final CompilationUnitTree compilationUnit;
        private final SourcePositions sourcePositions;

        private ParsedUnit(CompilationUnitTree compilationUnit, SourcePositions sourcePositions) {
            this.compilationUnit = compilationUnit;
            this.sourcePositions = sourcePositions;
        }
    }
}
