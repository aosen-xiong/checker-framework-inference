package checkers.inference.repair;

import com.sun.source.tree.VariableTree;

/** AST facts for a nullable field that participates in a repair candidate. */
public final class NullableFieldAstContext {
    private final VariableTree fieldTree;
    private final String fieldName;
    private final String declaredTypeSource;

    public NullableFieldAstContext(
            VariableTree fieldTree, String fieldName, String declaredTypeSource) {
        this.fieldTree = fieldTree;
        this.fieldName = fieldName;
        this.declaredTypeSource = declaredTypeSource;
    }

    public VariableTree getFieldTree() {
        return fieldTree;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDeclaredTypeSource() {
        return declaredTypeSource;
    }
}
