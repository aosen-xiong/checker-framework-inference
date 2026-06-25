package checkers.inference.repair;

import java.io.File;

/** One project-local usage example for an AI repair prompt. */
public final class RepairUsageExample {
    public enum Kind {
        SAFE,
        UNSAFE
    }

    private final Kind kind;
    private final File sourceFile;
    private final long lineNumber;
    private final String symbol;
    private final String source;

    public RepairUsageExample(
            Kind kind, File sourceFile, long lineNumber, String symbol, String source) {
        this.kind = kind;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.symbol = symbol;
        this.source = source;
    }

    public Kind getKind() {
        return kind;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSource() {
        return source;
    }

    public String summarize() {
        return kind.name()
                + " usage of "
                + symbol
                + " at "
                + sourceFile
                + ":"
                + lineNumber
                + "\n"
                + source;
    }
}
