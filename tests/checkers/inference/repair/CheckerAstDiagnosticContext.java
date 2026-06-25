package checkers.inference.repair;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Maps a checker diagnostic back to the javac AST node and source range it points at. */
public final class CheckerAstDiagnosticContext {
    private final RepairDiagnostic diagnostic;
    private final CompilationUnitTree compilationUnit;
    private final SourcePositions sourcePositions;
    private final TreePath diagnosticPath;

    private CheckerAstDiagnosticContext(
            RepairDiagnostic diagnostic,
            CompilationUnitTree compilationUnit,
            SourcePositions sourcePositions,
            TreePath diagnosticPath) {
        this.diagnostic = diagnostic;
        this.compilationUnit = compilationUnit;
        this.sourcePositions = sourcePositions;
        this.diagnosticPath = diagnosticPath;
    }

    public static CheckerAstDiagnosticContext from(RepairDiagnostic diagnostic) {
        ParsedUnit parsedUnit = parse(diagnostic.getSourceFile());
        long position = diagnostic.getPosition();
        if (position < 0 && diagnostic.getLineNumber() > 0) {
            long column = diagnostic.getColumnNumber() > 0 ? diagnostic.getColumnNumber() : 1;
            position =
                    parsedUnit.compilationUnit
                            .getLineMap()
                            .getPosition(diagnostic.getLineNumber(), column);
        }
        TreePath diagnosticPath =
                new SmallestContainingPathFinder(
                                parsedUnit.compilationUnit, parsedUnit.sourcePositions, position)
                        .find();
        if (diagnosticPath == null) {
            throw new IllegalArgumentException(
                    "Could not map diagnostic to AST path: "
                            + diagnostic.getSourceFile()
                            + ":"
                            + diagnostic.getLineNumber());
        }
        return new CheckerAstDiagnosticContext(
                diagnostic,
                parsedUnit.compilationUnit,
                parsedUnit.sourcePositions,
                diagnosticPath);
    }

    public RepairDiagnostic getDiagnostic() {
        return diagnostic;
    }

    public TreePath getDiagnosticPath() {
        return diagnosticPath;
    }

    public String getDiagnosticLeafKind() {
        return diagnosticPath.getLeaf().getKind().name();
    }

    public String getDiagnosticLeafSource() {
        return source(diagnosticPath.getLeaf());
    }

    public Optional<NullableDereferenceAstContext> nullableDereference() {
        TreePath path = diagnosticPath;
        while (path != null) {
            Tree leaf = path.getLeaf();
            if (leaf instanceof MethodInvocationTree) {
                MethodInvocationTree invocation = (MethodInvocationTree) leaf;
                Tree methodSelect = invocation.getMethodSelect();
                if (methodSelect instanceof MemberSelectTree) {
                    MemberSelectTree memberSelect = (MemberSelectTree) methodSelect;
                    return Optional.of(
                            new NullableDereferenceAstContext(
                                    leaf,
                                    memberSelect.getExpression(),
                                    source(leaf),
                                    source(memberSelect.getExpression())));
                }
            } else if (leaf instanceof MemberSelectTree) {
                MemberSelectTree memberSelect = (MemberSelectTree) leaf;
                TreePath parentPath = path.getParentPath();
                if (parentPath != null && parentPath.getLeaf() instanceof MethodInvocationTree) {
                    MethodInvocationTree invocation = (MethodInvocationTree) parentPath.getLeaf();
                    if (invocation.getMethodSelect() == leaf) {
                        return Optional.of(
                                new NullableDereferenceAstContext(
                                        invocation,
                                        memberSelect.getExpression(),
                                        source(invocation),
                                        source(memberSelect.getExpression())));
                    }
                }
                return Optional.of(
                        new NullableDereferenceAstContext(
                                leaf,
                                memberSelect.getExpression(),
                                source(leaf),
                                source(memberSelect.getExpression())));
            }
            path = path.getParentPath();
        }
        return Optional.empty();
    }

    public Optional<NullableFieldAstContext> nullableFieldForReceiver(
            NullableDereferenceAstContext dereference) {
        String receiverSource = dereference.getReceiverSource();
        if (!receiverSource.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            return Optional.empty();
        }
        NullableFieldFinder finder =
                new NullableFieldFinder(receiverSource, diagnostic.getPosition());
        finder.scan(compilationUnit, null);
        return Optional.ofNullable(finder.getFoundField());
    }

    private String source(Tree tree) {
        long start = sourcePositions.getStartPosition(compilationUnit, tree);
        long end = sourcePositions.getEndPosition(compilationUnit, tree);
        if (start < 0 || end < start) {
            return "";
        }
        StringBuilder source = new StringBuilder();
        List<String> lines = InferenceTestUtilities.getLines(diagnostic.getSourceFile());
        for (String line : lines) {
            source.append(line).append('\n');
        }
        return source.substring((int) start, (int) end).trim();
    }

    private String declaredTypeSource(VariableTree variableTree) {
        String typeSource = source(variableTree.getType());
        return typeSource.replaceAll("@[A-Za-z_$][A-Za-z0-9_$.]*\\s+", "").trim();
    }

    private static ParsedUnit parse(File sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required to parse repair sources.");
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

    private static final class SmallestContainingPathFinder extends TreePathScanner<Void, Void> {
        private final CompilationUnitTree compilationUnit;
        private final SourcePositions sourcePositions;
        private final long position;
        private TreePath bestPath;
        private long bestWidth = Long.MAX_VALUE;

        private SmallestContainingPathFinder(
                CompilationUnitTree compilationUnit, SourcePositions sourcePositions, long position) {
            this.compilationUnit = compilationUnit;
            this.sourcePositions = sourcePositions;
            this.position = position;
        }

        private TreePath find() {
            scan(compilationUnit, null);
            return bestPath;
        }

        @Override
        public Void scan(Tree tree, Void unused) {
            if (tree == null) {
                return null;
            }
            long start = sourcePositions.getStartPosition(compilationUnit, tree);
            long end = sourcePositions.getEndPosition(compilationUnit, tree);
            if (start <= position && position <= end) {
                long width = end - start;
                if (width >= 0 && width < bestWidth) {
                    bestWidth = width;
                    bestPath = new TreePath(getCurrentPath(), tree);
                }
                return super.scan(tree, unused);
            }
            return null;
        }
    }

    private final class NullableFieldFinder extends TreePathScanner<Void, Void> {
        private final String fieldName;
        private final long diagnosticPosition;
        private NullableFieldAstContext foundField;

        private NullableFieldFinder(String fieldName, long diagnosticPosition) {
            this.fieldName = fieldName;
            this.diagnosticPosition = diagnosticPosition;
        }

        @Override
        public Void visitVariable(VariableTree variableTree, Void unused) {
            if (foundField != null) {
                return null;
            }
            TreePath parentPath = getCurrentPath().getParentPath();
            if (parentPath == null || !(parentPath.getLeaf() instanceof ClassTree)) {
                return super.visitVariable(variableTree, unused);
            }
            long start = sourcePositions.getStartPosition(compilationUnit, variableTree);
            if (start > diagnosticPosition) {
                return super.visitVariable(variableTree, unused);
            }
            if (!fieldName.contentEquals(variableTree.getName())) {
                return super.visitVariable(variableTree, unused);
            }
            if (!hasNullableAnnotation(variableTree)) {
                return super.visitVariable(variableTree, unused);
            }
            foundField =
                    new NullableFieldAstContext(
                            variableTree, fieldName, declaredTypeSource(variableTree));
            return null;
        }

        private boolean hasNullableAnnotation(VariableTree variableTree) {
            if (source(variableTree.getType()).contains("@Nullable")) {
                return true;
            }
            for (AnnotationTree annotation : variableTree.getModifiers().getAnnotations()) {
                String annotationSource = annotation.getAnnotationType().toString();
                if (annotationSource.equals("Nullable") || annotationSource.endsWith(".Nullable")) {
                    return true;
                }
            }
            return false;
        }

        private NullableFieldAstContext getFoundField() {
            return foundField;
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
