package checkers.inference.repair;

import java.io.File;
import javax.tools.Diagnostic;

/** A checker diagnostic normalized for repair-search experiments. */
public final class RepairDiagnostic {
    private final String checkerName;
    private final File sourceFile;
    private final long lineNumber;
    private final long columnNumber;
    private final long position;
    private final Diagnostic.Kind kind;
    private final String key;
    private final String message;

    public RepairDiagnostic(
            String checkerName,
            File sourceFile,
            long lineNumber,
            long columnNumber,
            long position,
            Diagnostic.Kind kind,
            String key,
            String message) {
        this.checkerName = checkerName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.position = position;
        this.kind = kind;
        this.key = key;
        this.message = message;
    }

    public String getCheckerName() {
        return checkerName;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

    public long getPosition() {
        return position;
    }

    public Diagnostic.Kind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }
}
