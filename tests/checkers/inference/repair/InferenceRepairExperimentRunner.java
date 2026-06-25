package checkers.inference.repair;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceOptions.InitStatus;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Runs one inference-first repair experiment and returns a structured report. */
public final class InferenceRepairExperimentRunner {
    private final InferenceRepairExperimentConfig experimentConfig;
    private final InferenceRepairConfiguration configuration;
    private final InferenceRepairCandidatePlanner planner;
    private final InferenceRepairEditProvider editProvider;
    private final File outputDirectory;

    public InferenceRepairExperimentRunner(
            InferenceRepairConfiguration configuration,
            InferenceRepairCandidatePlanner planner,
            File outputDirectory) {
        this(
                InferenceRepairExperimentConfig.unsatCoreGuided(),
                configuration,
                planner,
                outputDirectory,
                new InferenceRepairEditGenerator());
    }

    public InferenceRepairExperimentRunner(
            InferenceRepairExperimentConfig experimentConfig,
            InferenceRepairConfiguration configuration,
            InferenceRepairCandidatePlanner planner,
            File outputDirectory,
            AiRepairClient aiRepairClient,
            List<File> projectSourceFiles) {
        this(
                experimentConfig,
                configuration,
                planner,
                outputDirectory,
                editProviderFor(experimentConfig, aiRepairClient, projectSourceFiles));
    }

    public InferenceRepairExperimentRunner(
            InferenceRepairConfiguration configuration,
            InferenceRepairCandidatePlanner planner,
            File outputDirectory,
            InferenceRepairEditProvider editProvider) {
        this(
                InferenceRepairExperimentConfig.unsatCoreGuided(),
                configuration,
                planner,
                outputDirectory,
                editProvider);
    }

    public InferenceRepairExperimentRunner(
            InferenceRepairExperimentConfig experimentConfig,
            InferenceRepairConfiguration configuration,
            InferenceRepairCandidatePlanner planner,
            File outputDirectory,
            InferenceRepairEditProvider editProvider) {
        this.experimentConfig = experimentConfig;
        this.configuration = configuration;
        this.planner = planner;
        this.outputDirectory = outputDirectory;
        this.editProvider = editProvider;
    }

    private static InferenceRepairEditProvider editProviderFor(
            InferenceRepairExperimentConfig experimentConfig,
            AiRepairClient aiRepairClient,
            List<File> projectSourceFiles) {
        InferenceRepairEditGenerator deterministicProvider = new InferenceRepairEditGenerator();
        AiRepairEditProvider aiProvider =
                new AiRepairEditProvider(
                        requireAiClient(experimentConfig, aiRepairClient), projectSourceFiles);
        if (experimentConfig.getEditProviderMode()
                == InferenceRepairEditProviderMode.DETERMINISTIC_ONLY) {
            return deterministicProvider;
        }
        if (experimentConfig.getEditProviderMode()
                == InferenceRepairEditProviderMode.AI_ONLY) {
            return aiProvider;
        }
        if (experimentConfig.getEditProviderMode()
                == InferenceRepairEditProviderMode.DETERMINISTIC_THEN_AI) {
            return new CompositeInferenceRepairEditProvider(
                    Arrays.<InferenceRepairEditProvider>asList(deterministicProvider, aiProvider));
        }
        return deterministicProvider;
    }

    private static AiRepairClient requireAiClient(
            InferenceRepairExperimentConfig experimentConfig, AiRepairClient aiRepairClient) {
        if (experimentConfig.getEditProviderMode()
                        == InferenceRepairEditProviderMode.DETERMINISTIC_ONLY
                && aiRepairClient == null) {
            return new EmptyAiRepairClient();
        }
        if (aiRepairClient == null) {
            throw new IllegalArgumentException(
                    "AI repair client is required for "
                            + experimentConfig.getEditProviderMode()
                            + " experiments.");
        }
        return aiRepairClient;
    }

    private static final class EmptyAiRepairClient implements AiRepairClient {
        @Override
        public List<String> proposeReplacementSources(RepairPromptContext context) {
            return Collections.emptyList();
        }
    }

    public InferenceRepairExperimentResult run(File sourceFile) {
        try {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new IllegalStateException(
                        "Could not create experiment output directory: "
                                + outputDirectory.getAbsolutePath());
            }
            InferenceRunSnapshot snapshot =
                    runInitialInference(sourceFile, new File(outputDirectory, "initial.jaif"));
            InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
            if (report.solverHadSolution()) {
                return new InferenceRepairExperimentResult(
                        experimentConfig,
                        sourceFile,
                        report,
                        new ArrayList<InferenceRepairCandidate>(),
                        new InferenceRepairSearchResult(
                                new ArrayList<InferenceRepairValidationResult>()));
            }

            List<InferenceRepairCandidate> candidates =
                    experimentConfig.shouldRunRepair()
                            ? planner.planFromInferenceContexts(repairContexts(report))
                            : new ArrayList<InferenceRepairCandidate>();
            InferenceRepairSearchResult searchResult =
                    experimentConfig.shouldRunRepair()
                            ? new InferenceRepairValidator(
                                            configuration,
                                            sourceFile,
                                            new File(outputDirectory, "validation"),
                                            editProvider)
                                    .validateAll(candidates)
                            : new InferenceRepairSearchResult(
                                    new ArrayList<InferenceRepairValidationResult>());
            return new InferenceRepairExperimentResult(
                    experimentConfig, sourceFile, report, candidates, searchResult);
        } catch (RuntimeException e) {
            return InferenceRepairExperimentResult.failed(experimentConfig, sourceFile, e);
        }
    }

    public InferenceRepairExperimentBatchResult runAll(List<File> sourceFiles) {
        return runAll(InferenceRepairExperimentMetadata.empty(), sourceFiles);
    }

    public InferenceRepairExperimentBatchResult runAll(
            InferenceRepairExperimentMetadata metadata, List<File> sourceFiles) {
        List<InferenceRepairExperimentResult> results = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            results.add(run(sourceFile));
        }
        return new InferenceRepairExperimentBatchResult(metadata, results);
    }

    private List<InferenceConstraintContext> repairContexts(InferenceConstraintReport report) {
        return experimentConfig.shouldExpandRepairContexts()
                ? report.getRepairConstraintContexts()
                : report.getUnsatConstraintContexts();
    }

    private InferenceRunSnapshot runInitialInference(File sourceFile, File jaifFile) {
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
