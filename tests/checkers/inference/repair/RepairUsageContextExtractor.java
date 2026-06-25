package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Source-based retrieval of safe and unsafe usages for repair prompt context. */
public final class RepairUsageContextExtractor implements RepairUsageIndex {
    private static final int MAX_EXAMPLES_PER_KIND = 3;
    private static final int MAX_EXAMPLE_LENGTH = 800;

    public List<RepairUsageExample> extract(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource,
            List<File> projectSourceFiles) {
        return findExamples(new RepairUsageQuery(candidate, target, originalSource, projectSourceFiles));
    }

    @Override
    public List<RepairUsageExample> findExamples(RepairUsageQuery query) {
        List<RepairUsageKey> keys = RepairUsageKey.fromTarget(query.getTarget());
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<RepairUsageExample> safeExamples = new ArrayList<>();
        List<RepairUsageExample> unsafeExamples = new ArrayList<>();
        for (File sourceFile : query.getProjectSourceFiles()) {
            String source =
                    readSource(
                            sourceFile,
                            query.getTarget().getSourceFile(),
                            query.getOriginalSource());
            collectExamples(sourceFile, source, keys, safeExamples, unsafeExamples);
        }

        List<RepairUsageExample> examples = new ArrayList<>();
        examples.addAll(firstExamples(safeExamples));
        examples.addAll(firstExamples(unsafeExamples));
        return examples;
    }

    private static void collectExamples(
            File sourceFile,
            String source,
            List<RepairUsageKey> keys,
            List<RepairUsageExample> safeExamples,
            List<RepairUsageExample> unsafeExamples) {
        for (RepairUsageKey key : keys) {
            int searchFrom = 0;
            while (searchFrom < source.length()) {
                int symbolOffset = source.indexOf(key.getSearchText(), searchFrom);
                if (symbolOffset < 0) {
                    break;
                }
                if (isSearchHit(source, symbolOffset, key)) {
                    SourceRegion region = regionContaining(source, symbolOffset);
                    if (region != null) {
                        String regionSource = bounded(region.source(source));
                        RepairUsageExample example =
                                new RepairUsageExample(
                                        isSafeUsage(regionSource, key)
                                                ? RepairUsageExample.Kind.SAFE
                                                : RepairUsageExample.Kind.UNSAFE,
                                        sourceFile,
                                        lineNumber(source, region.start),
                                        key.getDisplayName(),
                                        regionSource);
                        addIfNew(
                                example.getKind() == RepairUsageExample.Kind.SAFE
                                        ? safeExamples
                                        : unsafeExamples,
                                example);
                    }
                }
                searchFrom = symbolOffset + key.getSearchText().length();
            }
        }
    }

    private static boolean isSearchHit(String source, int offset, RepairUsageKey key) {
        if (!key.requiresIdentifierBoundary()) {
            return true;
        }
        int before = offset - 1;
        int after = offset + key.getSearchText().length();
        return (before < 0 || !Character.isJavaIdentifierPart(source.charAt(before)))
                && (after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after)));
    }

    private static SourceRegion regionContaining(String source, int offset) {
        int openBrace = source.lastIndexOf('{', offset);
        if (openBrace < 0) {
            return lineRegion(source, offset);
        }
        int signatureStart = source.lastIndexOf('\n', openBrace);
        while (signatureStart > 0) {
            int previousLineStart = source.lastIndexOf('\n', signatureStart - 1);
            String line = source.substring(previousLineStart + 1, signatureStart).trim();
            if (line.isEmpty() || line.startsWith("@")) {
                signatureStart = previousLineStart;
            } else {
                break;
            }
        }
        int closeBrace = matchingBrace(source, openBrace);
        if (closeBrace < offset) {
            return lineRegion(source, offset);
        }
        return new SourceRegion(Math.max(0, signatureStart + 1), closeBrace + 1);
    }

    private static SourceRegion lineRegion(String source, int offset) {
        int start = source.lastIndexOf('\n', offset);
        int end = source.indexOf('\n', offset);
        return new SourceRegion(start < 0 ? 0 : start + 1, end < 0 ? source.length() : end);
    }

    private static int matchingBrace(String source, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return source.length() - 1;
    }

    private static boolean isSafeUsage(String source, RepairUsageKey key) {
        String searchText = key.getSearchText();
        String displayName = key.getDisplayName();
        return containsNullCheck(source, searchText)
                || containsNullCheck(source, displayName)
                || source.contains("requireNonNull(" + searchText)
                || source.contains("requireNonNull(" + displayName)
                || source.contains("castToNonnull(" + searchText)
                || source.contains("castToNonnull(" + displayName);
    }

    private static boolean containsNullCheck(String source, String expression) {
        return source.contains(expression + " != null")
                || source.contains("null != " + expression)
                || source.contains(expression + " == null")
                || source.contains("null == " + expression);
    }

    private static void addIfNew(List<RepairUsageExample> examples, RepairUsageExample candidate) {
        for (RepairUsageExample example : examples) {
            if (example.getSourceFile().equals(candidate.getSourceFile())
                    && example.getLineNumber() == candidate.getLineNumber()
                    && example.getSource().equals(candidate.getSource())) {
                return;
            }
        }
        examples.add(candidate);
    }

    private static List<RepairUsageExample> firstExamples(List<RepairUsageExample> examples) {
        if (examples.size() <= MAX_EXAMPLES_PER_KIND) {
            return examples;
        }
        return new ArrayList<>(examples.subList(0, MAX_EXAMPLES_PER_KIND));
    }

    private static String bounded(String source) {
        String trimmed = source.trim();
        if (trimmed.length() <= MAX_EXAMPLE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_EXAMPLE_LENGTH) + "\n...";
    }

    private static long lineNumber(String source, int offset) {
        long line = 1;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String readSource(File sourceFile, File originalSourceFile, String originalSource) {
        if (sourceFile.equals(originalSourceFile)) {
            return originalSource;
        }
        try {
            return new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + sourceFile, e);
        }
    }

    private static final class SourceRegion {
        private final int start;
        private final int end;

        private SourceRegion(int start, int end) {
            this.start = start;
            this.end = end;
        }

        private String source(String fullSource) {
            return fullSource.substring(start, Math.min(end, fullSource.length()));
        }
    }
}
