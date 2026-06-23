package checkers.inference.repair;

import java.io.File;

/** A source-level type position that can participate in a repair constraint. */
public final class RepairSlot {
    private final String id;
    private final File sourceFile;
    private final int lineNumber;
    private final String role;
    private final String qualifier;

    public RepairSlot(String id, File sourceFile, int lineNumber, String role, String qualifier) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.role = role;
        this.qualifier = qualifier;
    }

    public String getId() {
        return id;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getRole() {
        return role;
    }

    public String getQualifier() {
        return qualifier;
    }

    @Override
    public String toString() {
        return id + "@" + sourceFile.getName() + ":" + lineNumber + "[" + qualifier + "]";
    }
}
