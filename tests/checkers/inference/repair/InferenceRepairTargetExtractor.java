package checkers.inference.repair;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Resolves inference repair candidates to parsed source targets. */
public final class InferenceRepairTargetExtractor {
    private static final Pattern BLOCK_STATEMENT_PATTERN =
            Pattern.compile("Block\\.statement\\s+(\\d+)");

    public InferenceRepairTarget extract(File sourceFile, InferenceRepairCandidate candidate) {
        int blockStatementIndex = blockStatementIndex(candidate.getTargetSlot().getLocation());
        ParsedUnit parsedUnit = parse(sourceFile);
        LocalVariableFinder finder = new LocalVariableFinder(blockStatementIndex);
        finder.scan(parsedUnit.compilationUnit, null);
        VariableTree variableTree = finder.getFoundVariable();
        if (variableTree == null) {
            throw new IllegalArgumentException(
                    "Could not resolve repair target from " + candidate.getTargetSlot().getLocation());
        }

        long start =
                parsedUnit.sourcePositions.getStartPosition(
                        parsedUnit.compilationUnit, variableTree);
        long end =
                parsedUnit.sourcePositions.getEndPosition(
                        parsedUnit.compilationUnit, variableTree);
        return new InferenceRepairTarget(
                sourceFile,
                variableTree.getKind().name(),
                start,
                end,
                parsedUnit.compilationUnit.getLineMap().getLineNumber(start),
                parsedUnit.compilationUnit.getLineMap().getColumnNumber(start),
                sourceSlice(sourceFile, start, end));
    }

    private static int blockStatementIndex(String location) {
        Matcher matcher = BLOCK_STATEMENT_PATTERN.matcher(location);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No Block.statement index in location: " + location);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static ParsedUnit parse(File sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required to parse repair targets.");
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

    private static String sourceSlice(File sourceFile, long start, long end) {
        StringBuilder source = new StringBuilder();
        List<String> lines = InferenceTestUtilities.getLines(sourceFile);
        for (String line : lines) {
            source.append(line).append('\n');
        }
        return source.substring((int) start, (int) end).trim();
    }

    private static final class LocalVariableFinder extends TreeScanner<VariableTree, Void> {
        private final int targetStatementIndex;
        private VariableTree foundVariable;

        private LocalVariableFinder(int targetStatementIndex) {
            this.targetStatementIndex = targetStatementIndex;
        }

        @Override
        public VariableTree visitMethod(MethodTree methodTree, Void unused) {
            if (foundVariable != null) {
                return foundVariable;
            }
            if (methodTree.getBody() == null) {
                return null;
            }
            List<? extends StatementTree> statements = methodTree.getBody().getStatements();
            if (targetStatementIndex >= statements.size()) {
                return super.visitMethod(methodTree, unused);
            }
            StatementTree statement = statements.get(targetStatementIndex);
            if (statement.getKind() == Tree.Kind.VARIABLE) {
                foundVariable = (VariableTree) statement;
                return foundVariable;
            }
            return super.visitMethod(methodTree, unused);
        }

        private VariableTree getFoundVariable() {
            return foundVariable;
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
