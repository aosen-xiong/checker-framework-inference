package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Classifies localization-study run errors into coarse benchmark-triage categories. */
final class RunErrorClassifier {
    private static final Pattern MISSING_SYMBOL_PATTERN =
            Pattern.compile("(?m)^\\s*symbol:\\s+(class|interface|enum|variable|method)\\s+([^\\s(]+)");
    private static final Pattern MISSING_PACKAGE_PATTERN =
            Pattern.compile("(?m)^.*error:\\s+package\\s+([^\\s]+)\\s+does not exist.*$");

    private RunErrorClassifier() {}

    static String classify(String runError) {
        if (runError == null || runError.isEmpty()) {
            return "none";
        }
        if (runError.startsWith("TIMEOUT")) {
            return "timeout";
        }
        if (runError.contains("cannot find symbol")) {
            return "javac-missing-symbol";
        }
        if (runError.contains("package ") && runError.contains(" does not exist")) {
            return "javac-missing-package";
        }
        if (runError.contains("javac failed during inference")) {
            return "javac-failed";
        }
        if (runError.contains("System.exit(") || runError.contains("System.exit")) {
            return "system-exit";
        }
        return "other";
    }

    static List<String> missingDependencyExamples(String runError, int limit) {
        List<String> examples = new ArrayList<>();
        if (runError == null || runError.isEmpty() || limit <= 0) {
            return examples;
        }
        String normalized = runError.replace("\\n", "\n").replace("\\r", "\r");
        appendMatches(examples, MISSING_PACKAGE_PATTERN.matcher(normalized), "package ", limit);
        appendMatches(examples, MISSING_SYMBOL_PATTERN.matcher(normalized), "", limit);
        return examples;
    }

    private static void appendMatches(
            List<String> examples, Matcher matcher, String prefix, int limit) {
        while (matcher.find() && examples.size() < limit) {
            String value;
            if (matcher.groupCount() == 1) {
                value = prefix + matcher.group(1);
            } else {
                value = matcher.group(1) + " " + matcher.group(2);
            }
            if (!examples.contains(value)) {
                examples.add(value);
            }
        }
    }
}
