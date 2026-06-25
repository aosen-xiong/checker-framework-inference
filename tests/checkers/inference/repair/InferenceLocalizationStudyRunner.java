package checkers.inference.repair;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceOptions;
import checkers.inference.InferenceRunSnapshot;
import checkers.inference.InferenceUnsatisfiableException;
import checkers.inference.InferenceOptions.InitStatus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/** Builds localization-study records from real CFI inference reports. */
public final class InferenceLocalizationStudyRunner {
    private static final int DEFAULT_TOP_K = 5;

    private final InferenceRepairConfiguration configuration;
    private final File outputDirectory;
    private final InferenceMcsEnumerator mcsEnumerator = new InferenceMcsEnumerator();
    private final SourceRepairUnitExtractor repairUnitExtractor = new SourceRepairUnitExtractor();
    private final SourceRepairPlanSelector repairPlanSelector = new SourceRepairPlanSelector();
    private final SourceRepairPlanValidator repairPlanValidator;
    private final int topK;
    private final boolean validateSourceRepairPlans;
    private final List<File> companionSourceFiles;
    private final List<String> extraJavacOptions;

    public InferenceLocalizationStudyRunner(File outputDirectory) {
        this(outputDirectory, DEFAULT_TOP_K);
    }

    public InferenceLocalizationStudyRunner(File outputDirectory, int topK) {
        this(outputDirectory, topK, false);
    }

    public InferenceLocalizationStudyRunner(
            File outputDirectory, int topK, boolean validateSourceRepairPlans) {
        this(outputDirectory, topK, validateSourceRepairPlans, Collections.<File>emptyList());
    }

    public InferenceLocalizationStudyRunner(
            File outputDirectory,
            int topK,
            boolean validateSourceRepairPlans,
            List<File> companionSourceFiles) {
        this(outputDirectory, topK, validateSourceRepairPlans, companionSourceFiles, Collections.<String>emptyList());
    }

    public InferenceLocalizationStudyRunner(
            File outputDirectory,
            int topK,
            boolean validateSourceRepairPlans,
            List<File> companionSourceFiles,
            List<String> extraJavacOptions) {
        this.configuration = InferenceRepairConfiguration.nninfDefault();
        this.outputDirectory = outputDirectory;
        this.topK = topK;
        this.validateSourceRepairPlans = validateSourceRepairPlans;
        this.companionSourceFiles = new ArrayList<>(companionSourceFiles);
        this.extraJavacOptions = new ArrayList<>(extraJavacOptions);
        this.repairPlanValidator = new SourceRepairPlanValidator(configuration);
    }

    public List<InferenceLocalizationStudyCase> runAll(List<File> sourceFiles) {
        List<InferenceLocalizationStudyCase> cases = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            cases.add(run(sourceFile));
        }
        return cases;
    }

    public InferenceLocalizationStudyCase run(File sourceFile) {
        try {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new IllegalStateException(
                        "Could not create localization-study work directory: "
                                + outputDirectory.getAbsolutePath());
            }
            InferenceRunSnapshot snapshot =
                    runInitialInference(sourceFile, new File(outputDirectory, "initial.jaif"));
            InferenceConstraintReport report = InferenceSnapshotReporter.report(snapshot);
            List<SourceRepairUnit> sourceRepairUnits =
                    repairUnitExtractor.extract(
                            mcsUniverse(
                                    report.getUnsatConstraintContexts(),
                                    report.getRepairConstraintContexts()));
            McsCandidateSummary mcsSummary =
                    weightedCandidates(
                            snapshot,
                            report.getUnsatConstraintContexts(),
                            report.getRepairConstraintContexts());
            List<SourceRepairPlan> sourceRepairPlans =
                    repairPlanSelector.selectTopK(sourceRepairUnits, topK);
            SourceRepairPlanValidationResult validationResult =
                    validateSourceRepairPlans
                            ? repairPlanValidator.validateTopPlan(
                                    sourceFile,
                                    new File(outputDirectory, "validated-source-repair"),
                                    sourceRepairPlans,
                                    report.getRepairConstraintContexts())
                            : null;
            return new InferenceLocalizationStudyCase(
                    sourceFile,
                    null,
                    report.solverHadSolution(),
                    report.getConstraintCount(),
                    report.getUnsatConstraintCount(),
                    mcsSummary.oracleKind,
                    mcsSummary.originalUniverseSize,
                    mcsSummary.enumeratedUniverseSize,
                    mcsSummary.universeTruncated,
                    mcsSummary.maxRemovalSize,
                    mcsSummary.searchBounded,
                    uniqueLocations(report.getUnsatConstraintContexts()),
                    sourceRepairUnits,
                    sourceRepairPlans,
                    validationResult,
                    mcsSummary.candidates);
        } catch (RuntimeException e) {
            return new InferenceLocalizationStudyCase(
                    sourceFile,
                    e.toString(),
                    false,
                    0,
                    0,
                    "NONE",
                    0,
                    0,
                    false,
                    0,
                    false,
                    Collections.<String>emptyList(),
                    Collections.<SourceRepairUnit>emptyList(),
                    Collections.<SourceRepairPlan>emptyList(),
                    Collections.<InferenceLocalizationStudyCandidate>emptyList());
        }
    }

    private McsCandidateSummary weightedCandidates(
            InferenceRunSnapshot snapshot,
            List<InferenceConstraintContext> unsatCoreContexts,
            List<InferenceConstraintContext> repairContexts) {
        String oracleKind =
                snapshot.getQualifierHierarchy() == null ? "REPORTED_CORE" : "SOLVER_BACKED";
        InferenceConstraintSatisfiabilityOracle oracle =
                snapshot.getQualifierHierarchy() == null
                        ? new ReportedCoreSatisfiabilityOracle(unsatCoreContexts)
                        : new SolverBackedInferenceConstraintOracle(
                                snapshot.getSlots(),
                                snapshot.getConstraints(),
                                snapshot.getQualifierHierarchy());
        InferenceMcsEnumerationResult enumerationResult =
                mcsEnumerator.enumerateTopKWithMetadata(
                        mcsUniverse(unsatCoreContexts, repairContexts),
                        oracle,
                        topK);
        List<InferenceLocalizationStudyCandidate> candidates = new ArrayList<>();
        for (InferenceMcsResult mcsResult : enumerationResult.getResults()) {
            List<SourceRepairUnit> candidateRepairUnits =
                    repairUnitExtractor.extract(mcsResult.getRemovedContexts());
            candidates.add(
                    new InferenceLocalizationStudyCandidate(
                            mcsResult.getRank(),
                            mcsResult.getWeight(),
                            candidateRepairUnits.size(),
                            sourceRealizableCount(candidateRepairUnits),
                            uniqueLocations(mcsResult.getRemovedContexts()),
                            evidence(mcsResult.getRemovedContexts())));
        }
        return new McsCandidateSummary(
                oracleKind,
                enumerationResult.getOriginalUniverseSize(),
                enumerationResult.getEnumeratedUniverseSize(),
                enumerationResult.isUniverseTruncated(),
                enumerationResult.getMaxRemovalSize(),
                enumerationResult.isSearchBounded(),
                candidates);
    }

    private InferenceRunSnapshot runInitialInference(File sourceFile, File jaifFile) {
        InitStatus status = InferenceOptions.init(inferenceArgs(sourceFile, jaifFile), false);
        status.validate();

        InferenceMain inferenceMain = InferenceMain.resetInstance();
        inferenceMain.setResultHandler(
                new InferenceMain.ResultHandler() {
                    @Override
                    public void handleCompilerResult(boolean success, String javacOutStr) {
                        if (!success) {
                            throw new RuntimeException(
                                    "javac failed during inference: "
                                            + singleLine(tail(javacOutStr, 20000)));
                        }
                    }
                });
        try {
            runQuietly(inferenceMain);
            return inferenceMain.getRunSnapshot();
        } catch (InferenceUnsatisfiableException e) {
            return e.getSnapshot();
        }
    }

    private static void runQuietly(InferenceMain inferenceMain) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        SecurityManager originalSecurityManager = System.getSecurityManager();
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        Logger rootLogger = Logger.getLogger("");
        Level originalRootLevel = rootLogger.getLevel();
        Handler[] handlers = rootLogger.getHandlers();
        Level[] originalHandlerLevels = new Level[handlers.length];
        for (int index = 0; index < handlers.length; index++) {
            originalHandlerLevels[index] = handlers[index].getLevel();
        }
        Map<Logger, Level> originalLoggerLevels = currentLoggerLevels();
        try {
            System.setOut(new PrintStream(capturedOut));
            System.setErr(new PrintStream(capturedErr));
            System.setSecurityManager(new NoExitSecurityManager(originalSecurityManager));
            rootLogger.setLevel(Level.SEVERE);
            for (Handler handler : handlers) {
                handler.setLevel(Level.SEVERE);
            }
            for (Logger logger : originalLoggerLevels.keySet()) {
                logger.setLevel(Level.OFF);
            }
            inferenceMain.run();
        } catch (InferenceUnsatisfiableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw enrichRunFailure(e, capturedOut, capturedErr);
        } finally {
            System.setSecurityManager(originalSecurityManager);
            System.setOut(originalOut);
            System.setErr(originalErr);
            for (Map.Entry<Logger, Level> entry : originalLoggerLevels.entrySet()) {
                entry.getKey().setLevel(entry.getValue());
            }
            rootLogger.setLevel(originalRootLevel);
            for (int index = 0; index < handlers.length; index++) {
                handlers[index].setLevel(originalHandlerLevels[index]);
            }
        }
    }

    private static RuntimeException enrichRunFailure(
            RuntimeException failure, ByteArrayOutputStream capturedOut, ByteArrayOutputStream capturedErr) {
        String stdout = tail(capturedOut.toString(), 2000);
        String stderr = tail(capturedErr.toString(), 4000);
        StringBuilder message = new StringBuilder(failure.toString());
        if (!stdout.trim().isEmpty()) {
            message.append("; stdout=").append(singleLine(stdout));
        }
        if (!stderr.trim().isEmpty()) {
            message.append("; stderr=").append(singleLine(stderr));
        }
        return new RuntimeException(message.toString(), failure);
    }

    private static String tail(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(value.length() - maxLength);
    }

    private static String singleLine(String value) {
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static final class NoExitSecurityManager extends SecurityManager {
        private final SecurityManager delegate;

        private NoExitSecurityManager(SecurityManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkPermission(Permission permission) {
            if (delegate != null) {
                delegate.checkPermission(permission);
            }
        }

        @Override
        public void checkPermission(Permission permission, Object context) {
            if (delegate != null) {
                delegate.checkPermission(permission, context);
            }
        }

        @Override
        public void checkExit(int status) {
            throw new SecurityException("System.exit(" + status + ") intercepted during inference run");
        }
    }

    private static Map<Logger, Level> currentLoggerLevels() {
        Map<Logger, Level> loggerLevels = new HashMap<>();
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            Logger logger = LogManager.getLogManager().getLogger(loggerNames.nextElement());
            if (logger != null) {
                loggerLevels.put(logger, logger.getLevel());
            }
        }
        return loggerLevels;
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
        args.add("--logLevel=OFF");
        args.add("--");
        args.addAll(configuration.getInferenceJavacOptions());
        args.addAll(extraJavacOptions);
        args.add(sourceFile.getPath());
        for (File companionSourceFile : companionSourceFiles) {
            if (!sameFile(sourceFile, companionSourceFile)) {
                args.add(companionSourceFile.getPath());
            }
        }
        return args.toArray(new String[args.size()]);
    }

    private static boolean sameFile(File first, File second) {
        return first.toPath().toAbsolutePath().normalize().equals(
                second.toPath().toAbsolutePath().normalize());
    }

    private static List<InferenceConstraintContext> mcsUniverse(
            List<InferenceConstraintContext> unsatCoreContexts,
            List<InferenceConstraintContext> repairContexts) {
        List<InferenceConstraintContext> universe = new ArrayList<>();
        for (InferenceConstraintContext context : repairContexts) {
            addUnique(universe, context);
        }
        for (InferenceConstraintContext context : unsatCoreContexts) {
            addUnique(universe, context);
        }
        return universe;
    }

    private static void addUnique(
            List<InferenceConstraintContext> contexts, InferenceConstraintContext candidate) {
        String key = candidate.summarize();
        for (InferenceConstraintContext context : contexts) {
            if (context.summarize().equals(key)) {
                return;
            }
        }
        contexts.add(candidate);
    }

    private static List<String> evidence(List<InferenceConstraintContext> contexts) {
        List<String> evidence = new ArrayList<>();
        for (InferenceConstraintContext context : contexts) {
            evidence.add(context.summarize());
        }
        return evidence;
    }

    private static List<String> uniqueLocations(List<InferenceConstraintContext> contexts) {
        List<String> locations = new ArrayList<>();
        for (InferenceConstraintContext context : contexts) {
            String location = InferenceMcsEnumerator.locationKey(context);
            if (!location.isEmpty() && !locations.contains(location)) {
                locations.add(location);
            }
        }
        return locations;
    }

    private static int sourceRealizableCount(List<SourceRepairUnit> units) {
        int count = 0;
        for (SourceRepairUnit unit : units) {
            if (unit.isSourceRealizable()) {
                count++;
            }
        }
        return count;
    }

    private static final class McsCandidateSummary {
        private final String oracleKind;
        private final int originalUniverseSize;
        private final int enumeratedUniverseSize;
        private final boolean universeTruncated;
        private final int maxRemovalSize;
        private final boolean searchBounded;
        private final List<InferenceLocalizationStudyCandidate> candidates;

        private McsCandidateSummary(
                String oracleKind,
                int originalUniverseSize,
                int enumeratedUniverseSize,
                boolean universeTruncated,
                int maxRemovalSize,
                boolean searchBounded,
                List<InferenceLocalizationStudyCandidate> candidates) {
            this.oracleKind = oracleKind;
            this.originalUniverseSize = originalUniverseSize;
            this.enumeratedUniverseSize = enumeratedUniverseSize;
            this.universeTruncated = universeTruncated;
            this.maxRemovalSize = maxRemovalSize;
            this.searchBounded = searchBounded;
            this.candidates = candidates;
        }
    }
}
