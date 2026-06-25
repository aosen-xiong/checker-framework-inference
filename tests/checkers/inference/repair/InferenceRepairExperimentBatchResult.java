package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Aggregate report for a set of inference-repair experiment inputs. */
public final class InferenceRepairExperimentBatchResult {
    private final InferenceRepairExperimentMetadata metadata;
    private final List<InferenceRepairExperimentResult> results;

    public InferenceRepairExperimentBatchResult(List<InferenceRepairExperimentResult> results) {
        this(InferenceRepairExperimentMetadata.empty(), results);
    }

    public InferenceRepairExperimentBatchResult(
            InferenceRepairExperimentMetadata metadata,
            List<InferenceRepairExperimentResult> results) {
        this.metadata = metadata;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    public InferenceRepairExperimentMetadata getMetadata() {
        return metadata;
    }

    public List<InferenceRepairExperimentResult> getResults() {
        return results;
    }

    public int inputCount() {
        return results.size();
    }

    public String modeName() {
        if (results.isEmpty()) {
            return "";
        }
        String mode = results.get(0).getExperimentConfig().getMode().name();
        for (InferenceRepairExperimentResult result : results) {
            if (!mode.equals(result.getExperimentConfig().getMode().name())) {
                return "MIXED";
            }
        }
        return mode;
    }

    public String editProviderModeName() {
        if (results.isEmpty()) {
            return "";
        }
        String mode = results.get(0).getExperimentConfig().getEditProviderMode().name();
        for (InferenceRepairExperimentResult result : results) {
            if (!mode.equals(result.getExperimentConfig().getEditProviderMode().name())) {
                return "MIXED";
            }
        }
        return mode;
    }

    public int runErrorCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            if (result.hasRunError()) {
                count++;
            }
        }
        return count;
    }

    public int initialInferenceSolvedCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            if (result.initialInferenceSolved()) {
                count++;
            }
        }
        return count;
    }

    public int repairSolvedInferenceCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            if (result.repairSolvedInference()) {
                count++;
            }
        }
        return count;
    }

    public int repairFullyVerifiedCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            if (result.repairFullyVerified()) {
                count++;
            }
        }
        return count;
    }

    public int candidateCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            count += result.getCandidates().size();
        }
        return count;
    }

    public int attemptCount() {
        int count = 0;
        for (InferenceRepairExperimentResult result : results) {
            count += result.attemptCount();
        }
        return count;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendStringField(builder, "projectName", metadata.getProjectName());
        appendStringField(builder, "projectRoot", metadata.projectRootPath());
        appendStringField(builder, "revision", metadata.getRevision());
        appendStringField(builder, "mode", modeName());
        appendStringField(builder, "editProviderMode", editProviderModeName());
        appendField(builder, "inputCount", inputCount());
        appendField(builder, "runErrorCount", runErrorCount());
        appendField(builder, "initialInferenceSolvedCount", initialInferenceSolvedCount());
        appendField(builder, "repairSolvedInferenceCount", repairSolvedInferenceCount());
        appendField(builder, "repairFullyVerifiedCount", repairFullyVerifiedCount());
        appendField(builder, "candidateCount", candidateCount());
        appendField(builder, "attemptCount", attemptCount());
        builder.append(",\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(results.get(i).toJson());
        }
        builder.append("]}");
        return builder.toString();
    }

    private static void appendField(StringBuilder builder, String name, int value) {
        if (builder.length() > 1) {
            builder.append(",");
        }
        builder.append("\"").append(name).append("\":").append(value);
    }

    private static void appendStringField(StringBuilder builder, String name, String value) {
        if (builder.length() > 1) {
            builder.append(",");
        }
        builder.append("\"").append(name).append("\":\"").append(value).append("\"");
    }
}
