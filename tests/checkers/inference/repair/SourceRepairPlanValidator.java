package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Validates source repair plans by materializing them and rerunning inference. */
public final class SourceRepairPlanValidator {
    private final SourceRepairPlanMaterializer materializer;
    private final InferenceRepairRunner runner;
    private final InferenceRepairConfiguration configuration;

    public SourceRepairPlanValidator(InferenceRepairConfiguration configuration) {
        this(
                new SourceRepairPlanMaterializer(),
                new DefaultInferenceRepairRunner(configuration),
                configuration);
    }

    SourceRepairPlanValidator(SourceRepairPlanMaterializer materializer, InferenceRepairRunner runner) {
        this(materializer, runner, InferenceRepairConfiguration.nninfDefault());
    }

    private SourceRepairPlanValidator(
            SourceRepairPlanMaterializer materializer,
            InferenceRepairRunner runner,
            InferenceRepairConfiguration configuration) {
        this.materializer = materializer;
        this.runner = runner;
        this.configuration = configuration;
    }

    public SourceRepairPlanValidationResult validateTopPlan(
            File originalSourceFile, File outputDirectory, List<SourceRepairPlan> plans) {
        return validateTopPlan(
                originalSourceFile,
                outputDirectory,
                plans,
                Collections.<InferenceConstraintContext>emptyList());
    }

    public SourceRepairPlanValidationResult validateTopPlan(
            File originalSourceFile,
            File outputDirectory,
            List<SourceRepairPlan> plans,
            List<InferenceConstraintContext> repairContexts) {
        if (plans.isEmpty()) {
            return SourceRepairPlanValidationResult.noPlan();
        }
        SourceRepairPlan plan = plans.get(0);
        File planDirectory = new File(outputDirectory, "source-repair-plan-" + plan.getRank());
        if (isExpressionRepairPlan(plan)) {
            return validateExpressionRepairPlan(originalSourceFile, planDirectory, plan, repairContexts);
        }
        SourceRepairPlanMaterialization materialization =
                materializer.materialize(plan, originalSourceFile, planDirectory);
        if (!materialization.isMaterialized()) {
            return SourceRepairPlanValidationResult.materializationFailed(
                    plan, materialization.getError());
        }
        try {
            InferenceRepairRunResult runResult =
                    runner.run(
                            materialization.getSourceFile(),
                            new File(planDirectory, "inference.jaif"),
                            new File(planDirectory, "annotated-source"));
            boolean inferenceSolved =
                    runResult.getSnapshot() != null && runResult.getSnapshot().hasSolution();
            InferenceRepairPostVerificationResult postVerificationResult =
                    runResult.getPostVerificationResult();
            boolean verified =
                    inferenceSolved
                            && postVerificationResult != null
                            && postVerificationResult.isVerified();
            SourceRepairPlanValidationResult validationResult =
                    SourceRepairPlanValidationResult.validated(
                    plan, materialization.getSourceFile(), inferenceSolved, verified, null);
            if (validationResult.isVerified()) {
                return validationResult;
            }
            return validateFollowUpDiagnosticRepair(
                    validationResult,
                    materialization.getSourceFile(),
                    planDirectory,
                    postVerificationResult);
        } catch (RuntimeException e) {
            return SourceRepairPlanValidationResult.validated(
                    plan, materialization.getSourceFile(), false, false, e.toString());
        }
    }

    private SourceRepairPlanValidationResult validateExpressionRepairPlan(
            File originalSourceFile,
            File planDirectory,
            SourceRepairPlan plan,
            List<InferenceConstraintContext> repairContexts) {
        try {
            List<InferenceRepairCandidate> candidates =
                    new SimpleNninfRepairPlanner().planFromInferenceContexts(repairContexts);
            InferenceRepairSearchResult searchResult =
                    new InferenceRepairValidator(
                                    configuration,
                                    originalSourceFile,
                                    new File(planDirectory, "inference-source-repair"))
                            .validateAll(candidates);
            InferenceRepairValidationResult verifiedResult = searchResult.getVerifiedResult();
            if (verifiedResult != null) {
                InferenceRepairAttempt attempt = verifiedResult.getVerifiedAttempt();
                return SourceRepairPlanValidationResult.sourceRepairValidated(
                        plan,
                        attempt.getRepairedSourceFile(),
                        attempt.solvesInference(),
                        attempt.isFullyVerified(),
                        attempt.getAppliedEdit(),
                        attempt.getReplacementSource(),
                        null);
            }
            InferenceRepairValidationResult passingResult = searchResult.getPassingResult();
            if (passingResult != null) {
                InferenceRepairAttempt attempt = passingResult.getPassingAttempt();
                return SourceRepairPlanValidationResult.sourceRepairValidated(
                        plan,
                        attempt.getRepairedSourceFile(),
                        attempt.solvesInference(),
                        false,
                        attempt.getAppliedEdit(),
                        attempt.getReplacementSource(),
                        "SOURCE_REPAIR_NOT_VERIFIED");
            }
            return SourceRepairPlanValidationResult.sourceRepairValidated(
                    plan, null, false, false, null, null, "SOURCE_REPAIR_UNSOLVED");
        } catch (RuntimeException e) {
            return SourceRepairPlanValidationResult.sourceRepairValidated(
                    plan, null, false, false, null, null, e.toString());
        }
    }

    private static boolean isExpressionRepairPlan(SourceRepairPlan plan) {
        return plan.getSteps().size() == 1
                && "RepairExpression".equals(plan.getSteps().get(0).getEditKind());
    }

    private SourceRepairPlanValidationResult validateFollowUpDiagnosticRepair(
            SourceRepairPlanValidationResult validationResult,
            File materializedSourceFile,
            File planDirectory,
            InferenceRepairPostVerificationResult postVerificationResult) {
        if (postVerificationResult == null || postVerificationResult.getTypecheckResult() == null) {
            return validationResult.withSkippedFollowUpRepair("NO_RESIDUAL_DIAGNOSTICS");
        }
        List<RepairDiagnostic> diagnostics =
                mapDiagnosticsToSource(
                        RepairDiagnosticAdapter.fromCaptureResult(
                                postVerificationResult.getTypecheckResult()),
                        materializedSourceFile);
        List<CodeRepairCandidate> candidates = new SimpleNninfCodeRepairPlanner().plan(diagnostics);
        SimpleNninfCodeRepairValidator validator =
                new SimpleNninfCodeRepairValidator(
                        configuration.getChecker(),
                        configuration.getTypecheckJavacOptions(),
                        new File(planDirectory, "follow-up-diagnostic-repair"));
        for (CodeRepairCandidate candidate : candidates) {
            CodeRepairValidationResult result = validator.validate(candidate);
            if (result.removesAllDiagnostics()) {
                return validationResult.withFollowUpRepair(
                        true, result.getRepairedSourceFile(), candidate.getDescription(), null);
            }
        }
        if (candidates.isEmpty()) {
            return validationResult.withSkippedFollowUpRepair("NO_SUPPORTED_DIAGNOSTIC_REPAIR");
        }
        return validationResult.withFollowUpRepair(false, null, "diagnostic repair attempted", null);
    }

    private static List<RepairDiagnostic> mapDiagnosticsToSource(
            List<RepairDiagnostic> diagnostics, File targetSourceFile) {
        List<RepairDiagnostic> mappedDiagnostics = new ArrayList<>();
        for (RepairDiagnostic diagnostic : diagnostics) {
            mappedDiagnostics.add(mapDiagnosticToSource(diagnostic, targetSourceFile));
        }
        return mappedDiagnostics;
    }

    private static RepairDiagnostic mapDiagnosticToSource(
            RepairDiagnostic diagnostic, File targetSourceFile) {
        int lineNumber = matchingLineNumber(diagnostic, targetSourceFile);
        long columnNumber = diagnostic.getColumnNumber() > 0 ? diagnostic.getColumnNumber() : 1;
        return new RepairDiagnostic(
                diagnostic.getCheckerName(),
                targetSourceFile,
                lineNumber,
                columnNumber,
                -1,
                diagnostic.getKind(),
                diagnostic.getKey(),
                diagnostic.getMessage());
    }

    private static int matchingLineNumber(RepairDiagnostic diagnostic, File targetSourceFile) {
        String diagnosticLine = line(diagnostic.getSourceFile(), diagnostic.getLineNumber()).trim();
        if (!diagnosticLine.isEmpty()) {
            List<String> targetLines = lines(targetSourceFile);
            for (int index = 0; index < targetLines.size(); index++) {
                if (diagnosticLine.equals(targetLines.get(index).trim())) {
                    return index + 1;
                }
            }
        }
        return (int) diagnostic.getLineNumber();
    }

    private static String line(File sourceFile, long lineNumber) {
        List<String> lines = lines(sourceFile);
        int index = (int) lineNumber - 1;
        if (index < 0 || index >= lines.size()) {
            return "";
        }
        return lines.get(index);
    }

    private static List<String> lines(File sourceFile) {
        try {
            return Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + sourceFile, e);
        }
    }
}
