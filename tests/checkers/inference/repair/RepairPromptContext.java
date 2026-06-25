package checkers.inference.repair;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** Compact, bounded context for AI-backed repair edit proposal. */
public final class RepairPromptContext {
    private static final int SURROUNDING_SOURCE_RADIUS = 400;
    private static final RepairUsageContextExtractor USAGE_CONTEXT_EXTRACTOR =
            new RepairUsageContextExtractor();

    private final File sourceFile;
    private final String candidateSummary;
    private final String constraintSummary;
    private final String targetSlotSummary;
    private final InferenceRepairKind repairKind;
    private final String qualifier;
    private final String targetTreeKind;
    private final long startOffset;
    private final long endOffset;
    private final long lineNumber;
    private final long columnNumber;
    private final String originalText;
    private final String expectedType;
    private final String surroundingSource;
    private final List<RepairUsageExample> usageExamples;

    public static RepairPromptContext create(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource) {
        return new RepairPromptContext(candidate, target, originalSource);
    }

    public static RepairPromptContext create(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource,
            List<File> projectSourceFiles) {
        return new RepairPromptContext(candidate, target, originalSource, projectSourceFiles);
    }

    private RepairPromptContext(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource) {
        this(
                candidate,
                target,
                originalSource,
                target.getSourceFile() == null
                        ? Collections.<File>emptyList()
                        : Collections.singletonList(target.getSourceFile()));
    }

    private RepairPromptContext(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource,
            List<File> projectSourceFiles) {
        this.sourceFile = target.getSourceFile();
        this.candidateSummary = candidate == null ? "" : candidate.summarize();
        this.constraintSummary =
                candidate == null || candidate.getConstraintContext() == null
                        ? ""
                        : candidate.getConstraintContext().summarize();
        this.targetSlotSummary =
                candidate == null || candidate.getTargetSlot() == null
                        ? ""
                        : candidate.getTargetSlot().summarize();
        this.repairKind =
                candidate == null
                        ? InferenceRepairKind.REPLACE_WITH_NONNULL_FALLBACK
                        : candidate.getRepairKind();
        this.qualifier = candidate == null ? "" : candidate.getQualifier();
        this.targetTreeKind = target.getTreeKind();
        this.startOffset = target.getStartOffset();
        this.endOffset = target.getEndOffset();
        this.lineNumber = target.getLineNumber();
        this.columnNumber = target.getColumnNumber();
        this.originalText = target.getOriginalText();
        this.expectedType = RepairTargetTypeResolver.expectedType(target, originalSource);
        this.surroundingSource = surroundingSource(target, originalSource);
        this.usageExamples =
                Collections.unmodifiableList(
                        USAGE_CONTEXT_EXTRACTOR.extract(
                                candidate, target, originalSource, projectSourceFiles));
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getCandidateSummary() {
        return candidateSummary;
    }

    public String getConstraintSummary() {
        return constraintSummary;
    }

    public String getTargetSlotSummary() {
        return targetSlotSummary;
    }

    public InferenceRepairKind getRepairKind() {
        return repairKind;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getTargetTreeKind() {
        return targetTreeKind;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public String getSurroundingSource() {
        return surroundingSource;
    }

    public List<RepairUsageExample> getUsageExamples() {
        return usageExamples;
    }

    public String toPromptText() {
        return "Repair only the allowed source span.\n"
                + "Return candidate replacement source text, not a full file.\n"
                + "Source file: "
                + sourceFile
                + "\nRepair kind: "
                + repairKind.name()
                + "\nQualifier: "
                + qualifier
                + "\nExpected type: "
                + nullToEmpty(expectedType)
                + "\nTarget: "
                + targetTreeKind
                + " at line "
                + lineNumber
                + ", column "
                + columnNumber
                + "\nAllowed replacement span offsets: "
                + startOffset
                + ".."
                + endOffset
                + "\nOriginal text:\n"
                + originalText
                + "\nConstraint:\n"
                + constraintSummary
                + "\nProject-local usage examples:\n"
                + usageExamplesText()
                + "\nSurrounding source:\n"
                + surroundingSource;
    }

    private String usageExamplesText() {
        if (usageExamples.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (RepairUsageExample example : usageExamples) {
            builder.append(example.summarize()).append("\n");
        }
        return builder.toString();
    }

    private static String surroundingSource(InferenceRepairTarget target, String originalSource) {
        int start = Math.max(0, checkedOffset(target.getStartOffset()) - SURROUNDING_SOURCE_RADIUS);
        int end =
                Math.min(
                        originalSource.length(),
                        checkedOffset(target.getEndOffset()) + SURROUNDING_SOURCE_RADIUS);
        return originalSource.substring(start, end);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int checkedOffset(long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid source offset: " + offset);
        }
        return (int) offset;
    }
}
