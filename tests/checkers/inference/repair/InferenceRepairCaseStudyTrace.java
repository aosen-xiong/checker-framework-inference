package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Renders one inference-repair run as a human-readable case-study trace. */
public final class InferenceRepairCaseStudyTrace {
    public String render(InferenceRepairExperimentResult result) {
        StringBuilder trace = new StringBuilder();
        trace.append("# Inference Repair Case Study\n\n");
        trace.append("Source: `").append(result.getSourceFile().getPath()).append("`\n");
        trace.append("Mode: `")
                .append(result.getExperimentConfig().getMode().name())
                .append("`\n\n");

        if (result.hasRunError()) {
            trace.append("## Run Error\n\n");
            trace.append(result.getRunError()).append("\n");
            return trace.toString();
        }

        appendInference(trace, result.getConstraintReport());
        appendCandidates(trace, result.getCandidates());
        appendValidation(trace, result.getSearchResult());
        appendConclusion(trace, result);
        return trace.toString();
    }

    private static void appendInference(StringBuilder trace, InferenceConstraintReport report) {
        trace.append("## Initial Inference\n\n");
        trace.append("- Solver had solution: `").append(report.solverHadSolution()).append("`\n");
        trace.append("- Slots: `").append(report.getSlotCount()).append("`\n");
        trace.append("- Constraints: `").append(report.getConstraintCount()).append("`\n");
        trace.append("- Unsat constraints: `").append(report.getUnsatConstraintCount()).append("`\n");
        trace.append("- Repair contexts: `")
                .append(report.getRepairConstraintContexts().size())
                .append("`\n\n");

        trace.append("### Unsat Constraint Contexts\n\n");
        appendContexts(trace, report.getUnsatConstraintContexts());

        trace.append("### Repair Constraint Contexts\n\n");
        appendContexts(trace, report.getRepairConstraintContexts());
    }

    private static void appendContexts(
            StringBuilder trace, List<InferenceConstraintContext> contexts) {
        if (contexts.isEmpty()) {
            trace.append("None.\n\n");
            return;
        }
        int index = 1;
        for (InferenceConstraintContext context : contexts) {
            trace.append(index++).append(". ").append(context.summarize()).append("\n");
        }
        trace.append("\n");
    }

    private static void appendCandidates(
            StringBuilder trace, List<InferenceRepairCandidate> candidates) {
        trace.append("## Repair Candidates\n\n");
        if (candidates.isEmpty()) {
            trace.append("No repair candidates were generated.\n\n");
            return;
        }
        int index = 1;
        for (InferenceRepairCandidate candidate : candidates) {
            trace.append(index++).append(". `")
                    .append(candidate.getRepairKind().name())
                    .append("`: ")
                    .append(candidate.summarize())
                    .append("\n");
        }
        trace.append("\n");
    }

    private static void appendValidation(
            StringBuilder trace, InferenceRepairSearchResult searchResult) {
        trace.append("## Validation\n\n");
        if (searchResult == null || searchResult.getValidationResults().isEmpty()) {
            trace.append("No repair validation was run.\n\n");
            return;
        }
        int candidateIndex = 1;
        for (InferenceRepairValidationResult validationResult :
                searchResult.getValidationResults()) {
            trace.append("### Candidate ").append(candidateIndex++).append("\n\n");
            trace.append(validationResult.getCandidate().summarize()).append("\n\n");
            if (validationResult.hasValidationError()) {
                trace.append("Validation error: `")
                        .append(validationResult.getValidationError())
                        .append("`\n\n");
                continue;
            }
            appendAttempts(trace, validationResult.getAttempts());
        }
    }

    private static void appendAttempts(
            StringBuilder trace, List<InferenceRepairAttempt> attempts) {
        if (attempts.isEmpty()) {
            trace.append("No concrete edits were attempted.\n\n");
            return;
        }
        int attemptIndex = 1;
        for (InferenceRepairAttempt attempt : attempts) {
            trace.append(attemptIndex++).append(". ")
                    .append(attempt.getAppliedEdit())
                    .append("\n");
            trace.append("   - Origin: `").append(attempt.getEditOrigin().name()).append("`\n");
            trace.append("   - Target: `").append(attempt.getTarget().summarize()).append("`\n");
            trace.append("   - Replacement: `")
                    .append(singleLine(attempt.getReplacementSource()))
                    .append("`\n");
            trace.append("   - Inference solved: `")
                    .append(attempt.solvesInference())
                    .append("`\n");
            trace.append("   - Final typecheck verified: `")
                    .append(attempt.isFullyVerified())
                    .append("`\n");
            trace.append("   - Repaired source: `")
                    .append(attempt.getRepairedSourceFile().getPath())
                    .append("`\n");
            trace.append("   - Before:\n\n");
            appendSourceSnippet(trace, attempt.getTarget().getSourceFile(), attempt.getTarget().getLineNumber());
            trace.append("   - After:\n\n");
            appendSourceSnippet(trace, attempt.getRepairedSourceFile(), attempt.getTarget().getLineNumber());
        }
        trace.append("\n");
    }

    private static void appendSourceSnippet(StringBuilder trace, File sourceFile, long targetLine) {
        List<String> lines;
        try {
            lines = Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            trace.append("```text\n")
                    .append("Could not read source snippet: ")
                    .append(e)
                    .append("\n```\n");
            return;
        }
        int center = (int) targetLine;
        int start = Math.max(1, center - 1);
        int end = Math.min(lines.size(), center + 1);
        trace.append("```java\n");
        for (int line = start; line <= end; line++) {
            trace.append(lines.get(line - 1)).append("\n");
        }
        trace.append("```\n");
    }

    private static void appendConclusion(
            StringBuilder trace, InferenceRepairExperimentResult result) {
        trace.append("## Conclusion\n\n");
        if (result.repairFullyVerified()) {
            InferenceRepairAttempt attempt =
                    result.getSearchResult().getVerifiedResult().getVerifiedAttempt();
            trace.append("Accepted edit: `")
                    .append(attempt.getAppliedEdit())
                    .append("`, replacing `")
                    .append(singleLine(attempt.getTarget().getOriginalText()))
                    .append("` with `")
                    .append(singleLine(attempt.getReplacementSource()))
                    .append("`.\n\n");
            trace.append("This case follows the intended pipeline: inference fails, the unsat context "
                    + "selects a source target, repair changes the source, inference succeeds, AFU inserts "
                    + "annotations, and the final checker typecheck passes.\n");
        } else if (result.repairSolvedInference()) {
            trace.append("A repair solved inference, but final annotation insertion or typechecking did not "
                    + "verify the result.\n");
        } else {
            trace.append("No attempted repair solved inference.\n");
        }
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "\\n");
    }
}
