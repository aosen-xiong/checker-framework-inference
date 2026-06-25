package checkers.inference.repair;

import java.io.File;

/** Concrete source file produced from one source repair plan. */
public final class SourceRepairPlanMaterialization {
    private final boolean materialized;
    private final File sourceFile;
    private final String error;

    private SourceRepairPlanMaterialization(boolean materialized, File sourceFile, String error) {
        this.materialized = materialized;
        this.sourceFile = sourceFile;
        this.error = error;
    }

    public static SourceRepairPlanMaterialization materialized(File sourceFile) {
        return new SourceRepairPlanMaterialization(true, sourceFile, null);
    }

    public static SourceRepairPlanMaterialization failed(String error) {
        return new SourceRepairPlanMaterialization(false, null, error);
    }

    public boolean isMaterialized() {
        return materialized;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getError() {
        return error;
    }
}
