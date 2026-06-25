package checkers.inference.repair;

import com.sun.source.tree.Tree;

/** AST facts for a nullable dereference diagnostic. */
public final class NullableDereferenceAstContext {
    private final Tree dereferenceTree;
    private final Tree receiverTree;
    private final String dereferenceSource;
    private final String receiverSource;

    public NullableDereferenceAstContext(
            Tree dereferenceTree, Tree receiverTree, String dereferenceSource, String receiverSource) {
        this.dereferenceTree = dereferenceTree;
        this.receiverTree = receiverTree;
        this.dereferenceSource = dereferenceSource;
        this.receiverSource = receiverSource;
    }

    public Tree getDereferenceTree() {
        return dereferenceTree;
    }

    public Tree getReceiverTree() {
        return receiverTree;
    }

    public String getDereferenceSource() {
        return dereferenceSource;
    }

    public String getReceiverSource() {
        return receiverSource;
    }
}
