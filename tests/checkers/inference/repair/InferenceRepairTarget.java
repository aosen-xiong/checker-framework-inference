package checkers.inference.repair;

import java.io.File;

/** Parsed source location that an inference-guided repair can edit. */
public final class InferenceRepairTarget {
    private final File sourceFile;
    private final String treeKind;
    private final long startOffset;
    private final long endOffset;
    private final long lineNumber;
    private final long columnNumber;
    private final String originalText;

    public InferenceRepairTarget(
            File sourceFile,
            String treeKind,
            long startOffset,
            long endOffset,
            long lineNumber,
            long columnNumber,
            String originalText) {
        this.sourceFile = sourceFile;
        this.treeKind = treeKind;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.originalText = originalText;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getTreeKind() {
        return treeKind;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String summarize() {
        return treeKind
                + " at "
                + sourceFile.getName()
                + ":"
                + lineNumber
                + ":"
                + columnNumber
                + " -> "
                + originalText;
    }
}
