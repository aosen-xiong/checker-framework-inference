package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Structured result for one inference-repair experiment input. */
public final class InferenceRepairExperimentResult {
    private final InferenceRepairExperimentConfig experimentConfig;
    private final File sourceFile;
    private final InferenceConstraintReport constraintReport;
    private final List<InferenceRepairCandidate> candidates;
    private final InferenceRepairSearchResult searchResult;
    private final RuntimeException runError;

    public InferenceRepairExperimentResult(
            InferenceRepairExperimentConfig experimentConfig,
            File sourceFile,
            InferenceConstraintReport constraintReport,
            List<InferenceRepairCandidate> candidates,
            InferenceRepairSearchResult searchResult) {
        this(experimentConfig, sourceFile, constraintReport, candidates, searchResult, null);
    }

    private InferenceRepairExperimentResult(
            InferenceRepairExperimentConfig experimentConfig,
            File sourceFile,
            InferenceConstraintReport constraintReport,
            List<InferenceRepairCandidate> candidates,
            InferenceRepairSearchResult searchResult,
            RuntimeException runError) {
        this.experimentConfig = experimentConfig;
        this.sourceFile = sourceFile;
        this.constraintReport = constraintReport;
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
        this.searchResult = searchResult;
        this.runError = runError;
    }

    public static InferenceRepairExperimentResult failed(
            InferenceRepairExperimentConfig experimentConfig,
            File sourceFile,
            RuntimeException runError) {
        return new InferenceRepairExperimentResult(
                experimentConfig,
                sourceFile,
                null,
                Collections.<InferenceRepairCandidate>emptyList(),
                null,
                runError);
    }

    public InferenceRepairExperimentConfig getExperimentConfig() {
        return experimentConfig;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public InferenceConstraintReport getConstraintReport() {
        return constraintReport;
    }

    public List<InferenceRepairCandidate> getCandidates() {
        return candidates;
    }

    public InferenceRepairSearchResult getSearchResult() {
        return searchResult;
    }

    public RuntimeException getRunError() {
        return runError;
    }

    public boolean hasRunError() {
        return runError != null;
    }

    public boolean initialInferenceSolved() {
        return constraintReport != null && constraintReport.solverHadSolution();
    }

    public boolean repairSolvedInference() {
        return searchResult != null && searchResult.solvesInference();
    }

    public boolean repairFullyVerified() {
        return searchResult != null && searchResult.isFullyVerified();
    }

    public int validationResultCount() {
        return searchResult == null ? 0 : searchResult.getValidationResults().size();
    }

    public int attemptCount() {
        if (searchResult == null) {
            return 0;
        }
        int attempts = 0;
        for (InferenceRepairValidationResult validationResult :
                searchResult.getValidationResults()) {
            attempts += validationResult.getAttempts().size();
        }
        return attempts;
    }

    public String toJson() {
        return new JsonBuilder()
                .beginObject()
                .name("mode").value(experimentConfig.getMode().name())
                .name("editProviderMode").value(experimentConfig.getEditProviderMode().name())
                .name("sourceFile").value(sourceFile.getPath())
                .name("runError").value(runError == null ? null : runError.toString())
                .name("initialInferenceSolved").value(initialInferenceSolved())
                .name("repairSolvedInference").value(repairSolvedInference())
                .name("repairFullyVerified").value(repairFullyVerified())
                .name("candidateCount").value(candidates.size())
                .name("validationResultCount").value(validationResultCount())
                .name("attemptCount").value(attemptCount())
                .name("constraints").constraintReport(constraintReport)
                .name("candidates").candidates(candidates)
                .name("validationResults").validationResults(searchResult)
                .endObject()
                .toString();
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

        private JsonBuilder constraintReport(InferenceConstraintReport report) {
            if (report == null) {
                return value((String) null);
            }
            beginObject()
                    .name("slotCount").value(report.getSlotCount())
                    .name("insertableSlotCount").value(report.getInsertableSlotCount())
                    .name("constraintCount").value(report.getConstraintCount())
                    .name("locatedConstraintCount").value(report.getLocatedConstraintCount())
                    .name("solverHadSolution").value(report.solverHadSolution())
                    .name("unsatConstraintCount").value(report.getUnsatConstraintCount())
                    .name("repairConstraintContextCount")
                    .value(report.getRepairConstraintContexts().size())
                    .name("constraintCountsByType");
            beginObject();
            for (String type : report.getConstraintCountsByType().keySet()) {
                name(type).value(report.getConstraintCountsByType().get(type));
            }
            endObject();
            return endObject();
        }

        private JsonBuilder candidates(List<InferenceRepairCandidate> candidates) {
            beginArray();
            for (InferenceRepairCandidate candidate : candidates) {
                beginObject()
                        .name("repairKind").value(candidate.getRepairKind().name())
                        .name("qualifier").value(candidate.getQualifier())
                        .name("description").value(candidate.getDescription())
                        .name("targetSlot").value(candidate.getTargetSlot().summarize())
                        .name("constraint").value(candidate.getConstraintContext().summarize())
                        .endObject();
            }
            return endArray();
        }

        private JsonBuilder validationResults(InferenceRepairSearchResult searchResult) {
            beginArray();
            if (searchResult != null) {
                for (InferenceRepairValidationResult validationResult :
                        searchResult.getValidationResults()) {
                    beginObject()
                            .name("hasValidationError").value(validationResult.hasValidationError())
                            .name("validationError")
                            .value(
                                    validationResult.getValidationError() == null
                                            ? null
                                            : validationResult.getValidationError().toString())
                            .name("solvesInference").value(validationResult.solvesInference())
                            .name("fullyVerified").value(validationResult.isFullyVerified())
                            .name("attempts");
                    beginArray();
                    for (InferenceRepairAttempt attempt : validationResult.getAttempts()) {
                        beginObject()
                                .name("repairKind").value(attempt.getRepairKind().name())
                                .name("editOrigin").value(attempt.getEditOrigin().name())
                                .name("appliedEdit").value(attempt.getAppliedEdit())
                                .name("replacementSource").value(attempt.getReplacementSource())
                                .name("target").value(attempt.getTarget().summarize())
                                .name("solvesInference").value(attempt.solvesInference())
                                .name("fullyVerified").value(attempt.isFullyVerified())
                                .endObject();
                    }
                    endArray();
                    endObject();
                }
            }
            return endArray();
        }

        private void beforeValue() {
            if (expectingNamedValue) {
                expectingNamedValue = false;
                return;
            }
            if (!firstStack.isEmpty()) {
                int last = firstStack.size() - 1;
                if (firstStack.get(last)) {
                    firstStack.set(last, Boolean.FALSE);
                } else {
                    builder.append(",");
                }
            }
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
