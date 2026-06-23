package checkers.inference.repair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates concrete source edits for one inference-guided repair target. */
public final class InferenceRepairEditGenerator {
    private static final Pattern NONNULL_STRING_LOCAL_PATTERN =
            Pattern.compile(
                    "(?m)^[ \\t]*(?:@NonNull\\s+)?String\\s+"
                            + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*[^;]+;");

    public List<InferenceRepairEdit> generate(
            InferenceRepairCandidate candidate, InferenceRepairTarget target, String originalSource) {
        List<InferenceRepairEdit> edits = new ArrayList<>();
        if (supportsRepairKind(candidate.getRepairKind(), target)) {
            edits.add(editFor(candidate.getRepairKind(), target, originalSource));
        }
        for (InferenceRepairEdit fallbackEdit : nonNullFallbackEdits(target, originalSource)) {
            if (!containsReplacement(edits, fallbackEdit.getReplacementSource())) {
                edits.add(fallbackEdit);
            }
        }
        return edits;
    }

    private static InferenceRepairEdit editFor(
            InferenceRepairKind repairKind, InferenceRepairTarget target, String originalSource) {
        if (repairKind == InferenceRepairKind.INSERT_NULL_GUARD) {
            String targetSource = sourceForTarget(target, originalSource);
            return new InferenceRepairEdit(
                    repairKind,
                    nullGuardFor(target, originalSource)
                            + "\n"
                            + indentationBefore(target, originalSource)
                            + targetSource,
                    "insert null guard before @NonNull local assignment",
                    "insert_null_guard");
        }
        if (repairKind == InferenceRepairKind.WEAKEN_ANNOTATION) {
            return new InferenceRepairEdit(
                    repairKind,
                    sourceForTarget(target, originalSource)
                            .replace("@NonNull String", "@Nullable String"),
                    "weaken @NonNull annotation to @Nullable",
                    "weaken_annotation");
        }
        throw new IllegalArgumentException("Unsupported direct repair kind: " + repairKind);
    }

    private static List<InferenceRepairEdit> nonNullFallbackEdits(
            InferenceRepairTarget target, String originalSource) {
        List<InferenceRepairEdit> edits = new ArrayList<>();
        String targetSource = sourceForTarget(target, originalSource);
        String inScopeStringLocal = inScopeStringLocalBefore(target, originalSource);
        if (inScopeStringLocal != null) {
            edits.add(
                    new InferenceRepairEdit(
                            InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                            replaceWithFallback(targetSource, inScopeStringLocal),
                            "replace nullable expression with in-scope non-null String local",
                            "replace_with_string_local"));
        }
        edits.add(
                new InferenceRepairEdit(
                        InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK,
                        replaceWithFallback(targetSource, "\"\""),
                        "replace nullable expression with non-null String literal",
                        "replace_with_string_literal"));
        return edits;
    }

    private static boolean supportsRepairKind(
            InferenceRepairKind repairKind, InferenceRepairTarget target) {
        return repairKind != InferenceRepairKind.INSERT_NULL_GUARD
                || "VARIABLE".equals(target.getTreeKind());
    }

    private static boolean containsReplacement(List<InferenceRepairEdit> edits, String replacement) {
        for (InferenceRepairEdit edit : edits) {
            if (edit.getReplacementSource().equals(replacement)) {
                return true;
            }
        }
        return false;
    }

    private static String inScopeStringLocalBefore(
            InferenceRepairTarget target, String originalSource) {
        String methodPrefix = methodPrefixBefore(target, originalSource);
        Matcher matcher = NONNULL_STRING_LOCAL_PATTERN.matcher(methodPrefix);
        String localName = null;
        while (matcher.find()) {
            localName = matcher.group(1);
        }
        return localName;
    }

    private static String methodPrefixBefore(InferenceRepairTarget target, String originalSource) {
        int targetStart = checkedOffset(target.getStartOffset());
        int methodStart = originalSource.lastIndexOf('{', targetStart);
        if (methodStart < 0) {
            return originalSource.substring(0, targetStart);
        }
        return originalSource.substring(methodStart + 1, targetStart);
    }

    private static String replaceWithFallback(String targetSource, String fallbackSource) {
        if (!targetSource.contains("=")) {
            return fallbackSource;
        }
        return targetSource.substring(0, targetSource.indexOf('=') + 1)
                + " "
                + fallbackSource
                + ";";
    }

    private static String nullGuardFor(InferenceRepairTarget target, String originalSource) {
        String targetSource = sourceForTarget(target, originalSource);
        String rhs = targetSource.substring(targetSource.indexOf('=') + 1).replace(";", "").trim();
        return "if (" + rhs + " == null) { return; }";
    }

    private static String indentationBefore(InferenceRepairTarget target, String source) {
        int start = checkedOffset(target.getStartOffset());
        int lineStart = source.lastIndexOf('\n', start - 1) + 1;
        return source.substring(lineStart, start);
    }

    private static String sourceForTarget(InferenceRepairTarget target, String originalSource) {
        return originalSource.substring(
                checkedOffset(target.getStartOffset()), checkedOffset(target.getEndOffset()));
    }

    private static int checkedOffset(long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid source offset: " + offset);
        }
        return (int) offset;
    }
}
