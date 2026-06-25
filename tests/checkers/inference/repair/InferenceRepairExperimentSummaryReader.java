package checkers.inference.repair;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Reads top-level metrics from experiment JSON reports. */
public final class InferenceRepairExperimentSummaryReader {
    public InferenceRepairExperimentSummary read(File reportFile) {
        String json;
        try {
            json = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read experiment report: " + reportFile, e);
        }
        JsonObject report = JsonParser.parseString(json).getAsJsonObject();
        return new InferenceRepairExperimentSummary(
                reportFile,
                stringField(report, "projectName"),
                stringField(report, "revision"),
                stringField(report, "mode"),
                stringField(report, "editProviderMode"),
                intField(report, "inputCount"),
                intField(report, "runErrorCount"),
                intField(report, "initialInferenceSolvedCount"),
                intField(report, "repairSolvedInferenceCount"),
                intField(report, "repairFullyVerifiedCount"),
                intField(report, "candidateCount"),
                intField(report, "attemptCount"));
    }

    private static String stringField(JsonObject report, String fieldName) {
        if (!report.has(fieldName) || report.get(fieldName).isJsonNull()) {
            return "";
        }
        return report.get(fieldName).getAsString();
    }

    private static int intField(JsonObject report, String fieldName) {
        if (!report.has(fieldName) || report.get(fieldName).isJsonNull()) {
            return 0;
        }
        return report.get(fieldName).getAsInt();
    }
}
