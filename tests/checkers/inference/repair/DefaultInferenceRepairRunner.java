package checkers.inference.repair;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Default repair runner backed by the real inference compiler pipeline. */
public final class DefaultInferenceRepairRunner implements InferenceRepairRunner {
    private final InferenceRepairConfiguration configuration;
    private final InferenceRepairPostVerifier postVerifier;

    public DefaultInferenceRepairRunner(InferenceRepairConfiguration configuration) {
        this.configuration = configuration;
        this.postVerifier = new InferenceRepairPostVerifier(configuration);
    }

    @Override
    public InferenceRepairRunResult run(
            File repairedSourceFile, File jaifFile, File annotatedSourceDirectory) {
        InferenceRunSnapshot snapshot = runInference(repairedSourceFile, jaifFile);
        InferenceRepairPostVerificationResult postVerificationResult =
                snapshot != null && snapshot.hasSolution()
                        ? postVerifier.verify(
                                repairedSourceFile, jaifFile, annotatedSourceDirectory)
                        : null;
        return new InferenceRepairRunResult(snapshot, postVerificationResult);
    }

    private InferenceRunSnapshot runInference(File sourceFile, File jaifFile) {
        InitStatus status = InferenceOptions.init(inferenceArgs(sourceFile, jaifFile), false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        try {
            inferenceMain.run();
            return inferenceMain.getRunSnapshot();
        } catch (InferenceUnsatisfiableException e) {
            return e.getSnapshot();
        }
    }

    private String[] inferenceArgs(File sourceFile, File jaifFile) {
        List<String> args = new ArrayList<>();
        args.add("--checker");
        args.add(configuration.getChecker().getCanonicalName());
        args.add("--solver");
        args.add(configuration.getSolver());
        args.add("--jaifFile");
        args.add(jaifFile.getAbsolutePath());
        if (configuration.shouldUseHacks()) {
            args.add("--hacks=true");
        }
        args.add("--");
        args.addAll(configuration.getInferenceJavacOptions());
        args.add(sourceFile.getPath());
        return args.toArray(new String[args.size()]);
    }
}
