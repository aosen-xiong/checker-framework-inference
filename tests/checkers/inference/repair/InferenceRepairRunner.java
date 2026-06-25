package checkers.inference.repair;

import java.io.File;

/** Executes inference and optional post-verification for one repaired source attempt. */
public interface InferenceRepairRunner {
    InferenceRepairRunResult run(
            File repairedSourceFile, File jaifFile, File annotatedSourceDirectory);
}
