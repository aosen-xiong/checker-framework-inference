package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parser for NullRepair's tab-separated manual-inspection scoring export. */
public final class NullRepairManualInspectionParser {
    private NullRepairManualInspectionParser() {}

    public static List<NullRepairManualInspectionCase> parse(File inputFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read manual-inspection TSV: " + inputFile, e);
        }
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        String[] headers = lines.get(0).split("\t", -1);
        Map<String, Integer> headerIndexes = headerIndexes(headers);
        List<NullRepairManualInspectionCase> cases = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] values = line.split("\t", -1);
            cases.add(
                    new NullRepairManualInspectionCase(
                            value(values, headerIndexes, "Benchmark"),
                            value(values, headerIndexes, "ID"),
                            value(values, headerIndexes, "Type"),
                            value(values, headerIndexes, "Message"),
                            value(values, headerIndexes, "Path"),
                            value(values, headerIndexes, "Expression"),
                            intValue(values, headerIndexes, "SCORE (Patch A) Consolidated"),
                            intValue(values, headerIndexes, "SCORE (Patch B) Consolidated"),
                            intValue(values, headerIndexes, "SCORE (Patch C) Consolidated")));
        }
        return cases;
    }

    private static Map<String, Integer> headerIndexes(String[] headers) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            indexes.put(headers[i], i);
        }
        require(indexes, "Benchmark");
        require(indexes, "ID");
        require(indexes, "Type");
        require(indexes, "Message");
        require(indexes, "Path");
        require(indexes, "Expression");
        require(indexes, "SCORE (Patch A) Consolidated");
        require(indexes, "SCORE (Patch B) Consolidated");
        require(indexes, "SCORE (Patch C) Consolidated");
        return indexes;
    }

    private static void require(Map<String, Integer> indexes, String header) {
        if (!indexes.containsKey(header)) {
            throw new IllegalArgumentException("Missing TSV header: " + header);
        }
    }

    private static String value(String[] values, Map<String, Integer> headerIndexes, String header) {
        int index = headerIndexes.get(header);
        return index < values.length ? values[index] : "";
    }

    private static int intValue(String[] values, Map<String, Integer> headerIndexes, String header) {
        String value = value(values, headerIndexes, header);
        if (value.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(value);
    }
}
