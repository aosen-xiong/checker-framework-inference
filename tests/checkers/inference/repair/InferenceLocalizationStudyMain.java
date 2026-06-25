package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** Command-line entry point for the CFI nullability localization pilot-study report. */
public final class InferenceLocalizationStudyMain {
    private InferenceLocalizationStudyMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        if (arguments.workerSourceFile != null) {
            InferenceLocalizationStudyCase studyCase =
                    new InferenceLocalizationStudyRunner(
                                    arguments.workDirectory(),
                                    arguments.topK,
                                    arguments.validateSourceRepairPlans,
                                    arguments.companionSourceFiles,
                                    arguments.javacOptions)
                            .run(arguments.workerSourceFile);
            writeReport(arguments.workerOutputFile, caseToJson(studyCase));
            if (arguments.workerCsvOutputFile != null) {
                writeReport(arguments.workerCsvOutputFile, toCsvRow(arguments, studyCase) + "\n");
            }
            return;
        }
        List<File> sampledSources =
                sample(arguments.sourceFiles, arguments.sampleSize, arguments.seed);
        List<WorkerOutput> workerOutputs =
                arguments.inProcess
                        ? runInProcess(arguments, sampledSources)
                        : runWorkers(arguments, sampledSources);
        writeReport(arguments.outputFile, toJson(arguments, sampledSources, workerOutputs));
        if (arguments.csvOutputFile != null) {
            writeReport(arguments.csvOutputFile, toCsv(arguments, workerOutputs));
        }
    }

    private static List<WorkerOutput> runInProcess(Arguments arguments, List<File> sampledSources) {
        List<InferenceLocalizationStudyCase> cases =
                new InferenceLocalizationStudyRunner(
                                arguments.workDirectory(),
                                arguments.topK,
                                arguments.validateSourceRepairPlans,
                                arguments.companionSourceFiles,
                                arguments.javacOptions)
                        .runAll(sampledSources);
        List<WorkerOutput> outputs = new ArrayList<>();
        for (InferenceLocalizationStudyCase studyCase : cases) {
            outputs.add(new WorkerOutput(caseToJson(studyCase), toCsvRow(arguments, studyCase)));
        }
        return outputs;
    }

    private static List<WorkerOutput> runWorkers(Arguments arguments, List<File> sampledSources) {
        List<WorkerOutput> outputs = new ArrayList<>();
        for (int index = 0; index < sampledSources.size(); index++) {
            File sourceFile = sampledSources.get(index);
            File workerJson = new File(arguments.workDirectory(), "case-" + index + ".json");
            File workerCsv = new File(arguments.workDirectory(), "case-" + index + ".csv");
            String workerError = runWorker(arguments, sourceFile, workerJson, workerCsv);
            if (workerError == null && workerJson.exists() && workerCsv.exists()) {
                outputs.add(
                        new WorkerOutput(readFile(workerJson), firstLine(workerCsv)));
            } else {
                String error =
                        workerError == null
                                ? "WORKER_FAILED without output"
                                : workerError;
                InferenceLocalizationStudyCase timeoutCase =
                        new InferenceLocalizationStudyCase(
                                sourceFile,
                                error,
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
                                null,
                                Collections.<InferenceLocalizationStudyCandidate>emptyList());
                outputs.add(
                        new WorkerOutput(
                                caseToJson(timeoutCase), toCsvRow(arguments, timeoutCase)));
            }
        }
        return outputs;
    }

    private static String runWorker(
            Arguments arguments, File sourceFile, File workerJson, File workerCsv) {
        File workDirectory = arguments.workDirectory();
        if (!workDirectory.exists() && !workDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create localization-study worker directory: "
                            + workDirectory.getAbsolutePath());
        }
        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        String afuScriptsPath = System.getProperty("path.afu.scripts");
        if (afuScriptsPath != null) {
            command.add("-Dpath.afu.scripts=" + afuScriptsPath);
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(InferenceLocalizationStudyMain.class.getCanonicalName());
        command.add("--worker-source");
        command.add(sourceFile.getPath());
        command.add("--worker-out");
        command.add(workerJson.getPath());
        command.add("--worker-csv-out");
        command.add(workerCsv.getPath());
        command.add("--out");
        command.add(arguments.outputFile.getPath());
        command.add("--sample-size");
        command.add("1");
        command.add("--top-k");
        command.add(String.valueOf(arguments.topK));
        if (arguments.validateSourceRepairPlans) {
            command.add("--validate-source-repair-plans");
        }
        for (File companionSourceListFile : arguments.companionSourceListFiles) {
            command.add("--companion-source-list");
            command.add(companionSourceListFile.getPath());
        }
        if (!arguments.projectName.isEmpty()) {
            command.add("--project-name");
            command.add(arguments.projectName);
        }
        if (!arguments.revision.isEmpty()) {
            command.add("--revision");
            command.add(arguments.revision);
        }
        if (arguments.projectRoot != null) {
            command.add("--project-root");
            command.add(arguments.projectRoot.getPath());
        }
        for (String javacOption : arguments.javacOptions) {
            command.add("--javac-option");
            command.add(javacOption);
        }
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectOutput(new File(workDirectory, workerJson.getName() + ".out"));
            processBuilder.redirectError(new File(workDirectory, workerJson.getName() + ".err"));
            process = processBuilder.start();
            boolean finished = process.waitFor(arguments.timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "TIMEOUT after " + arguments.timeoutSeconds + " seconds";
            }
            int exitValue = process.exitValue();
            return exitValue == 0 ? null : "WORKER_FAILED exit " + exitValue;
        } catch (IOException | InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "WORKER_FAILED " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static List<File> sample(List<File> sourceFiles, int sampleSize, long seed) {
        List<File> sampledSources = new ArrayList<>(sourceFiles);
        Collections.shuffle(sampledSources, new Random(seed));
        if (sampleSize > 0 && sampleSize < sampledSources.size()) {
            return new ArrayList<>(sampledSources.subList(0, sampleSize));
        }
        return sampledSources;
    }

    private static String toJson(
            Arguments arguments,
            List<File> sampledSources,
            List<WorkerOutput> workerOutputs) {
        JsonBuilder json = new JsonBuilder();
        json.beginObject()
                .name("projectName").value(arguments.projectName)
                .name("projectRoot").value(arguments.projectRoot == null ? null : arguments.projectRoot.getPath())
                .name("revision").value(arguments.revision)
                .name("seed").value(arguments.seed)
                .name("requestedSampleSize").value(arguments.sampleSize)
                .name("sampledInputCount").value(sampledSources.size())
                .name("sourceFilter").value(arguments.sourceFilter.name())
                .name("topK").value(arguments.topK)
                .name("candidateSemantics")
                .value("top-k weighted minimal correction sets from a solver-backed retained-constraint oracle; falls back to reported-core oracle only when raw solver context is unavailable")
                .name("allowedRepairSchemas");
        stringArray(json, LocalizationStudyAnnotationSchema.REPAIR_SCHEMAS);
        json.name("allowedRepairScopeLabels");
        stringArray(json, LocalizationStudyAnnotationSchema.REPAIR_SCOPE_LABELS);
        json.name("allowedHitLabels");
        stringArray(json, LocalizationStudyAnnotationSchema.HIT_LABELS);
        json
                .name("cases");
        json.beginArray();
        for (WorkerOutput workerOutput : workerOutputs) {
            json.rawValue(workerOutput.caseJson);
        }
        json.endArray();
        return json.endObject().toString();
    }

    private static String caseToJson(InferenceLocalizationStudyCase studyCase) {
        JsonBuilder json = new JsonBuilder();
        caseJson(json, studyCase);
        return json.toString();
    }

    private static void caseJson(JsonBuilder json, InferenceLocalizationStudyCase studyCase) {
        json.beginObject()
                .name("sourceFile").value(studyCase.getSourceFile().getPath())
                .name("runError").value(studyCase.getRunError())
                .name("solverHadSolution").value(studyCase.solverHadSolution())
                .name("constraintCount").value(studyCase.getConstraintCount())
                .name("unsatConstraintCount").value(studyCase.getUnsatConstraintCount())
                .name("mcsOracleKind").value(studyCase.getMcsOracleKind())
                .name("mcsOriginalUniverseSize").value(studyCase.getMcsOriginalUniverseSize())
                .name("mcsEnumeratedUniverseSize").value(studyCase.getMcsEnumeratedUniverseSize())
                .name("mcsUniverseTruncated").value(studyCase.isMcsUniverseTruncated())
                .name("mcsMaxRemovalSize").value(studyCase.getMcsMaxRemovalSize())
                .name("mcsSearchBounded").value(studyCase.isMcsSearchBounded())
                .name("sourceRepairUnitCount").value(studyCase.getSourceRepairUnitCount())
                .name("sourceRealizableRepairUnitCount")
                .value(studyCase.getSourceRealizableRepairUnitCount())
                .name("hasSourceRealizableRepairUnit")
                .value(studyCase.hasSourceRealizableRepairUnit())
                .name("unsatCoreLocations");
        stringArray(json, studyCase.getUnsatCoreLocations());
        json.name("sourceRepairUnits").beginArray();
        for (SourceRepairUnit unit : studyCase.getSourceRepairUnits()) {
            json.beginObject()
                    .name("id").value(unit.getId())
                    .name("kind").value(unit.getKind())
                    .name("provenanceKind").value(unit.getProvenanceKind())
                    .name("sourceRealizable").value(unit.isSourceRealizable())
                    .name("location").value(unit.getLocation())
                    .name("evidence").value(unit.getEvidence())
                    .name("editDomain")
                    .beginArray();
            for (SourceRepairEditOption option : unit.getEditDomain()) {
                json.beginObject()
                        .name("kind").value(option.getKind())
                        .name("qualifier").value(option.getQualifier())
                        .name("cost").value(option.getCost())
                        .endObject();
            }
            json.endArray().endObject();
        }
        json.endArray();
        json.name("sourceRepairPlans").beginArray();
        for (SourceRepairPlan plan : studyCase.getSourceRepairPlans()) {
            json.beginObject()
                    .name("rank").value(plan.getRank())
                    .name("totalCost").value(plan.getTotalCost())
                    .name("steps")
                    .beginArray();
            for (SourceRepairPlanStep step : plan.getSteps()) {
                json.beginObject()
                        .name("repairUnitId").value(step.getRepairUnitId())
                        .name("editKind").value(step.getEditKind())
                        .name("qualifier").value(step.getQualifier())
                        .name("cost").value(step.getCost())
                        .endObject();
            }
            json.endArray().endObject();
        }
        json.endArray();
        SourceRepairPlanValidationResult validationResult =
                studyCase.getSourceRepairPlanValidationResult();
        json.name("sourceRepairPlanValidation");
        if (validationResult == null) {
            json.value((String) null);
        } else {
            json.beginObject()
                    .name("planRank")
                    .value(validationResult.getPlan() == null ? 0 : validationResult.getPlan().getRank())
                    .name("materialized")
                    .value(validationResult.isMaterialized())
                    .name("repairedSourceFile")
                    .value(
                            validationResult.getRepairedSourceFile() == null
                                    ? null
                                    : validationResult.getRepairedSourceFile().getPath())
                    .name("inferenceSolved")
                    .value(validationResult.isInferenceSolved())
                    .name("verified")
                    .value(validationResult.isVerified())
                    .name("followUpRepairAttempted")
                    .value(validationResult.isFollowUpRepairAttempted())
                    .name("followUpRepairVerified")
                    .value(validationResult.isFollowUpRepairVerified())
                    .name("followUpRepairedSourceFile")
                    .value(
                            validationResult.getFollowUpRepairedSourceFile() == null
                                    ? null
                                    : validationResult.getFollowUpRepairedSourceFile().getPath())
                    .name("followUpAppliedEdit")
                    .value(validationResult.getFollowUpAppliedEdit())
                    .name("followUpReplacementSource")
                    .value(validationResult.getFollowUpReplacementSource())
                    .name("error")
                    .value(validationResult.getError())
                    .name("stages")
                    .beginArray();
            for (RepairStageTrace stage : validationResult.getStages()) {
                json.beginObject()
                        .name("stage")
                        .value(stage.getStage())
                        .name("action")
                        .value(stage.getAction())
                        .name("attempted")
                        .value(stage.isAttempted())
                        .name("succeeded")
                        .value(stage.isSucceeded())
                        .name("verified")
                        .value(stage.isVerified())
                        .name("outputFile")
                        .value(
                                stage.getOutputFile() == null
                                        ? null
                                        : stage.getOutputFile().getPath())
                        .name("appliedEdit")
                        .value(stage.getAppliedEdit())
                        .name("replacementSource")
                        .value(stage.getReplacementSource())
                        .name("error")
                        .value(stage.getError())
                        .endObject();
            }
            json.endArray().endObject();
        }
        json.name("weightedMcsCandidates").beginArray();
        for (InferenceLocalizationStudyCandidate candidate :
                studyCase.getWeightedMcsCandidates()) {
            json.beginObject()
                    .name("rank").value(candidate.getRank())
                    .name("weight").value(candidate.getWeight())
                    .name("sourceRepairUnitCount").value(candidate.getSourceRepairUnitCount())
                    .name("sourceRealizableRepairUnitCount")
                    .value(candidate.getSourceRealizableRepairUnitCount())
                    .name("hasSourceRealizableRepairUnit")
                    .value(candidate.hasSourceRealizableRepairUnit())
                    .name("locations");
            stringArray(json, candidate.getLocations());
            json.name("evidence");
            stringArray(json, candidate.getEvidence());
            json.endObject();
        }
        json.endArray()
                .name("humanRepairLocations");
        json.beginArray().endArray()
                .name("humanRepairUnitIds");
        json.beginArray().endArray()
                .name("humanRepairSchemas");
        json.beginArray().endArray()
                .name("humanRepairNotes").value("")
                .name("top1Hit").value((String) null)
                .name("top3Hit").value((String) null)
                .name("top5Hit").value((String) null)
                .name("sourceRealizableTop1Hit").value((String) null)
                .name("sourceRealizableTop3Hit").value((String) null)
                .name("sourceRealizableTop5Hit").value((String) null)
                .endObject();
    }

    private static String toCsv(Arguments arguments, List<WorkerOutput> workerOutputs) {
        StringBuilder csv = new StringBuilder();
        csv.append("projectName,revision,sourceFile,runError,solverHadSolution,constraintCount,")
                .append("unsatConstraintCount,mcsOracleKind,mcsOriginalUniverseSize,")
                .append("mcsEnumeratedUniverseSize,mcsUniverseTruncated,mcsMaxRemovalSize,")
                .append("mcsSearchBounded,sourceRepairUnitCount,")
                .append("sourceRealizableRepairUnitCount,hasSourceRealizableRepairUnit,")
                .append("unsatCoreLocations");
        for (int rank = 1; rank <= arguments.topK; rank++) {
            csv.append(",top")
                    .append(rank)
                    .append("Weight,top")
                    .append(rank)
                    .append("SourceRepairUnitCount,top")
                    .append(rank)
                    .append("SourceRealizableRepairUnitCount,top")
                    .append(rank)
                    .append("HasSourceRealizableRepairUnit,top")
                    .append(rank)
                    .append("Locations,top")
                    .append(rank)
                    .append("Evidence");
        }
        csv.append(",humanRepairLocations,humanRepairUnitIds,humanRepairSchemas,humanRepairNotes,")
                .append("top1Hit,top3Hit,top5Hit,sourceRealizableTop1Hit,")
                .append("sourceRealizableTop3Hit,sourceRealizableTop5Hit,")
                .append("sourceRepairPlanMaterialized,sourceRepairPlanInferenceSolved,")
                .append("sourceRepairPlanVerified,sourceRepairFollowUpAttempted,")
                .append("sourceRepairFollowUpVerified,sourceRepairFollowUpAppliedEdit,")
                .append("sourceRepairFollowUpReplacementSource,")
                .append("sourceRepairPlanValidationError\n");
        for (WorkerOutput workerOutput : workerOutputs) {
            csv.append(workerOutput.csvRow).append("\n");
        }
        return csv.toString();
    }

    private static String toCsvRow(
            Arguments arguments, InferenceLocalizationStudyCase studyCase) {
        StringBuilder csv = new StringBuilder();
        csv.append(csv(arguments.projectName))
                .append(",")
                .append(csv(arguments.revision))
                .append(",")
                .append(csv(studyCase.getSourceFile().getPath()))
                .append(",")
                .append(csv(studyCase.getRunError()))
                .append(",")
                .append(studyCase.solverHadSolution())
                .append(",")
                .append(studyCase.getConstraintCount())
                .append(",")
                .append(studyCase.getUnsatConstraintCount())
                .append(",")
                .append(csv(studyCase.getMcsOracleKind()))
                .append(",")
                .append(studyCase.getMcsOriginalUniverseSize())
                .append(",")
                .append(studyCase.getMcsEnumeratedUniverseSize())
                .append(",")
                .append(studyCase.isMcsUniverseTruncated())
                .append(",")
                .append(studyCase.getMcsMaxRemovalSize())
                .append(",")
                .append(studyCase.isMcsSearchBounded())
                .append(",")
                .append(studyCase.getSourceRepairUnitCount())
                .append(",")
                .append(studyCase.getSourceRealizableRepairUnitCount())
                .append(",")
                .append(studyCase.hasSourceRealizableRepairUnit())
                .append(",")
                .append(csv(join(studyCase.getUnsatCoreLocations())));
        for (int rank = 1; rank <= arguments.topK; rank++) {
            InferenceLocalizationStudyCandidate candidate = candidateAtRank(studyCase, rank);
            csv.append(",");
            if (candidate == null) {
                csv.append(csv(""))
                        .append(",")
                        .append(csv(""))
                        .append(",")
                        .append(csv(""))
                        .append(",")
                        .append(csv(""))
                        .append(",")
                        .append(csv(""))
                        .append(",")
                        .append(csv(""));
            } else {
                csv.append(candidate.getWeight())
                        .append(",")
                        .append(candidate.getSourceRepairUnitCount())
                        .append(",")
                        .append(candidate.getSourceRealizableRepairUnitCount())
                        .append(",")
                        .append(candidate.hasSourceRealizableRepairUnit())
                        .append(",")
                        .append(csv(join(candidate.getLocations())))
                        .append(",")
                        .append(csv(join(candidate.getEvidence())));
            }
        }
        csv.append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""))
                .append(",")
                .append(csv(""));
        SourceRepairPlanValidationResult validationResult =
                studyCase.getSourceRepairPlanValidationResult();
        csv.append(",");
        if (validationResult == null) {
            csv.append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""))
                    .append(",")
                    .append(csv(""));
        } else {
            csv.append(validationResult.isMaterialized())
                    .append(",")
                    .append(validationResult.isInferenceSolved())
                    .append(",")
                    .append(validationResult.isVerified())
                    .append(",")
                    .append(validationResult.isFollowUpRepairAttempted())
                    .append(",")
                    .append(validationResult.isFollowUpRepairVerified())
                    .append(",")
                    .append(csv(validationResult.getFollowUpAppliedEdit()))
                    .append(",")
                    .append(csv(validationResult.getFollowUpReplacementSource()))
                    .append(",")
                    .append(csv(validationResult.getError()));
        }
        return csv.toString();
    }

    private static InferenceLocalizationStudyCandidate candidateAtRank(
            InferenceLocalizationStudyCase studyCase, int rank) {
        for (InferenceLocalizationStudyCandidate candidate :
                studyCase.getWeightedMcsCandidates()) {
            if (candidate.getRank() == rank) {
                return candidate;
            }
        }
        return null;
    }

    private static String join(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                joined.append(" | ");
            }
            joined.append(values.get(index));
        }
        return joined.toString();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static void stringArray(JsonBuilder json, List<String> values) {
        json.beginArray();
        for (String value : values) {
            json.value(value);
        }
        json.endArray();
    }

    private static void writeReport(File outputFile, String json) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create localization-study output directory: "
                            + parent.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write localization-study report: " + outputFile, e);
        }
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("Could not read worker output: " + file, e);
        }
    }

    private static String firstLine(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            return lines.isEmpty() ? "" : lines.get(0);
        } catch (IOException e) {
            throw new RuntimeException("Could not read worker CSV output: " + file, e);
        }
    }

    private static final class WorkerOutput {
        private final String caseJson;
        private final String csvRow;

        private WorkerOutput(String caseJson, String csvRow) {
            this.caseJson = caseJson;
            this.csvRow = csvRow;
        }
    }

    private static final class Arguments {
        private final String projectName;
        private final File projectRoot;
        private final String revision;
        private final File outputFile;
        private final File csvOutputFile;
        private final File workerSourceFile;
        private final File workerOutputFile;
        private final File workerCsvOutputFile;
        private final int sampleSize;
        private final long seed;
        private final int topK;
        private final int timeoutSeconds;
        private final boolean inProcess;
        private final boolean validateSourceRepairPlans;
        private final InferenceRepairSourceDiscovery.SourceFilter sourceFilter;
        private final List<File> sourceFiles;
        private final List<File> companionSourceFiles;
        private final List<File> companionSourceListFiles;
        private final List<String> javacOptions;

        private Arguments(
                String projectName,
                File projectRoot,
                String revision,
                File outputFile,
                File csvOutputFile,
                File workerSourceFile,
                File workerOutputFile,
                File workerCsvOutputFile,
                int sampleSize,
                long seed,
                int topK,
                int timeoutSeconds,
                boolean inProcess,
                boolean validateSourceRepairPlans,
                InferenceRepairSourceDiscovery.SourceFilter sourceFilter,
                List<File> sourceFiles,
                List<File> companionSourceFiles,
                List<File> companionSourceListFiles,
                List<String> javacOptions) {
            this.projectName = projectName;
            this.projectRoot = projectRoot;
            this.revision = revision;
            this.outputFile = outputFile;
            this.csvOutputFile = csvOutputFile;
            this.workerSourceFile = workerSourceFile;
            this.workerOutputFile = workerOutputFile;
            this.workerCsvOutputFile = workerCsvOutputFile;
            this.sampleSize = sampleSize;
            this.seed = seed;
            this.topK = topK;
            this.timeoutSeconds = timeoutSeconds;
            this.inProcess = inProcess;
            this.validateSourceRepairPlans = validateSourceRepairPlans;
            this.sourceFilter = sourceFilter;
            this.sourceFiles = sourceFiles;
            this.companionSourceFiles = companionSourceFiles;
            this.companionSourceListFiles = companionSourceListFiles;
            this.javacOptions = javacOptions;
        }

        private File workDirectory() {
            File parent = outputFile.getParentFile();
            return parent == null
                    ? new File("build/inference-localization-study/work")
                    : new File(parent, "work");
        }

        private static Arguments parse(String[] args) {
            String projectName = "";
            File projectRoot = null;
            String revision = "";
            File outputFile = new File("build/inference-localization-study/report.json");
            File csvOutputFile = null;
            File workerSourceFile = null;
            File workerOutputFile = null;
            File workerCsvOutputFile = null;
            int sampleSize = 100;
            long seed = 1L;
            int topK = 5;
            int timeoutSeconds = 60;
            boolean inProcess = false;
            boolean validateSourceRepairPlans = false;
            InferenceRepairSourceDiscovery.SourceFilter sourceFilter =
                    InferenceRepairSourceDiscovery.SourceFilter.ALL;
            List<File> sourceFiles = new ArrayList<>();
            List<File> sourceListFiles = new ArrayList<>();
            List<File> companionSourceFiles = new ArrayList<>();
            List<File> companionSourceListFiles = new ArrayList<>();
            List<File> sourceRoots = new ArrayList<>();
            List<String> javacOptions = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--out".equals(arg)) {
                    outputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--csv-out".equals(arg)) {
                    csvOutputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--worker-source".equals(arg)) {
                    workerSourceFile = new File(requiredValue(args, ++i, arg));
                } else if ("--worker-out".equals(arg)) {
                    workerOutputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--worker-csv-out".equals(arg)) {
                    workerCsvOutputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--project-name".equals(arg)) {
                    projectName = requiredValue(args, ++i, arg);
                } else if ("--project-root".equals(arg)) {
                    projectRoot = new File(requiredValue(args, ++i, arg));
                } else if ("--revision".equals(arg)) {
                    revision = requiredValue(args, ++i, arg);
                } else if ("--sample-size".equals(arg)) {
                    sampleSize = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--seed".equals(arg)) {
                    seed = Long.parseLong(requiredValue(args, ++i, arg));
                } else if ("--top-k".equals(arg)) {
                    topK = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--timeout-seconds".equals(arg)) {
                    timeoutSeconds = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--in-process".equals(arg)) {
                    inProcess = true;
                } else if ("--validate-source-repair-plans".equals(arg)) {
                    validateSourceRepairPlans = true;
                } else if ("--source-filter".equals(arg)) {
                    sourceFilter =
                            InferenceRepairSourceDiscovery.SourceFilter.valueOf(
                                    requiredValue(args, ++i, arg));
                } else if ("--source".equals(arg)) {
                    sourceFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--source-list".equals(arg)) {
                    sourceListFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--companion-source".equals(arg)) {
                    companionSourceFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--companion-source-list".equals(arg)) {
                    companionSourceListFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--source-root".equals(arg)) {
                    sourceRoots.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--javac-option".equals(arg)) {
                    javacOptions.add(requiredValue(args, ++i, arg));
                } else if ("--javac-options-file".equals(arg)) {
                    javacOptions.addAll(readOptionFile(new File(requiredValue(args, ++i, arg))));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            for (File sourceListFile : sourceListFiles) {
                sourceFiles.addAll(
                        InferenceRepairSourceDiscovery.readSourceList(sourceListFile, projectRoot));
            }
            for (File companionSourceListFile : companionSourceListFiles) {
                companionSourceFiles.addAll(
                        InferenceRepairSourceDiscovery.readSourceList(
                                companionSourceListFile, projectRoot));
            }
            sourceFiles.addAll(
                    InferenceRepairSourceDiscovery.discoverJavaSources(sourceRoots, sourceFilter));
            if (workerSourceFile != null && workerOutputFile == null) {
                throw new IllegalArgumentException("--worker-out is required with --worker-source.");
            }
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException("--timeout-seconds must be positive.");
            }
            if (workerSourceFile == null && sourceFiles.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one --source, --source-list, or --source-root argument is required.");
            }
            return new Arguments(
                    projectName,
                    projectRoot,
                    revision,
                    outputFile,
                    csvOutputFile,
                    workerSourceFile,
                    workerOutputFile,
                    workerCsvOutputFile,
                    sampleSize,
                    seed,
                    topK,
                    timeoutSeconds,
                    inProcess,
                    validateSourceRepairPlans,
                    sourceFilter,
                    sourceFiles,
                    companionSourceFiles,
                    companionSourceListFiles,
                    javacOptions);
        }

        private static List<String> readOptionFile(File optionFile) {
            List<String> options = new ArrayList<>();
            List<String> lines;
            try {
                lines = Files.readAllLines(optionFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not read javac options file: " + optionFile, e);
            }
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    options.add(trimmed);
                }
            }
            return options;
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }

    private static final class JsonBuilder {
        private final StringBuilder builder = new StringBuilder();
        private final List<Boolean> firstStack = new ArrayList<>();
        private boolean expectingNamedValue;

        private JsonBuilder beginObject() {
            beforeValue();
            builder.append("{");
            firstStack.add(Boolean.TRUE);
            return this;
        }

        private JsonBuilder endObject() {
            builder.append("}");
            firstStack.remove(firstStack.size() - 1);
            return this;
        }

        private JsonBuilder beginArray() {
            beforeValue();
            builder.append("[");
            firstStack.add(Boolean.TRUE);
            return this;
        }

        private JsonBuilder endArray() {
            builder.append("]");
            firstStack.remove(firstStack.size() - 1);
            return this;
        }

        private JsonBuilder name(String name) {
            beforeValue();
            builder.append("\"").append(escape(name)).append("\":");
            expectingNamedValue = true;
            return this;
        }

        private JsonBuilder value(String value) {
            beforeValue();
            if (value == null) {
                builder.append("null");
            } else {
                builder.append("\"").append(escape(value)).append("\"");
            }
            return this;
        }

        private JsonBuilder value(boolean value) {
            beforeValue();
            builder.append(value);
            return this;
        }

        private JsonBuilder value(int value) {
            beforeValue();
            builder.append(value);
            return this;
        }

        private JsonBuilder value(long value) {
            beforeValue();
            builder.append(value);
            return this;
        }

        private JsonBuilder rawValue(String value) {
            beforeValue();
            builder.append(value);
            return this;
        }

        private void beforeValue() {
            if (expectingNamedValue) {
                expectingNamedValue = false;
                return;
            }
            if (firstStack.isEmpty()) {
                return;
            }
            int top = firstStack.size() - 1;
            if (firstStack.get(top)) {
                firstStack.set(top, Boolean.FALSE);
            } else {
                builder.append(",");
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
