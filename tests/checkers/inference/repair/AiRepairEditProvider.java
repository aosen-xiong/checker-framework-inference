package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Adapts AI replacement-source proposals into repair edits validated by the normal pipeline. */
public final class AiRepairEditProvider implements AiInferenceRepairEditProvider {
    private static final int DEFAULT_MAX_EDITS = 5;
    private static final int DEFAULT_MAX_REPLACEMENT_LENGTH = 500;

    private final AiRepairClient client;
    private final int maxEdits;
    private final int maxReplacementLength;
    private final List<File> projectSourceFiles;

    public AiRepairEditProvider(AiRepairClient client) {
        this(client, DEFAULT_MAX_EDITS, DEFAULT_MAX_REPLACEMENT_LENGTH);
    }

    public AiRepairEditProvider(AiRepairClient client, List<File> projectSourceFiles) {
        this(client, DEFAULT_MAX_EDITS, DEFAULT_MAX_REPLACEMENT_LENGTH, projectSourceFiles);
    }

    public AiRepairEditProvider(AiRepairClient client, int maxEdits) {
        this(client, maxEdits, DEFAULT_MAX_REPLACEMENT_LENGTH);
    }

    public AiRepairEditProvider(AiRepairClient client, int maxEdits, int maxReplacementLength) {
        this(client, maxEdits, maxReplacementLength, Collections.<File>emptyList());
    }

    public AiRepairEditProvider(
            AiRepairClient client,
            int maxEdits,
            int maxReplacementLength,
            List<File> projectSourceFiles) {
        if (maxEdits <= 0) {
            throw new IllegalArgumentException("maxEdits must be positive.");
        }
        if (maxReplacementLength <= 0) {
            throw new IllegalArgumentException("maxReplacementLength must be positive.");
        }
        this.client = client;
        this.maxEdits = maxEdits;
        this.maxReplacementLength = maxReplacementLength;
        this.projectSourceFiles =
                Collections.unmodifiableList(new ArrayList<File>(projectSourceFiles));
    }

    @Override
    public List<InferenceRepairEdit> generate(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource) {
        RepairPromptContext context =
                projectSourceFiles.isEmpty()
                        ? RepairPromptContext.create(candidate, target, originalSource)
                        : RepairPromptContext.create(
                                candidate, target, originalSource, projectSourceFiles);
        return generate(context);
    }

    @Override
    public List<InferenceRepairEdit> generate(RepairPromptContext context) {
        List<String> replacementSources;
        try {
            replacementSources = client.proposeReplacementSources(context);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }

        List<InferenceRepairEdit> edits = new ArrayList<>();
        for (String replacementSource : replacementSources) {
            String sanitizedReplacement = sanitizeReplacement(replacementSource);
            if (!sanitizedReplacement.isEmpty()
                    && sanitizedReplacement.length() <= maxReplacementLength
                    && !sanitizedReplacement.equals(context.getOriginalText())
                    && !containsReplacement(edits, sanitizedReplacement)) {
                edits.add(
                        new InferenceRepairEdit(
                                context.getRepairKind(),
                                sanitizedReplacement,
                                "AI proposed replacement for repair target",
                                "ai_replacement_" + edits.size(),
                                InferenceRepairEditOrigin.AI));
                if (edits.size() >= maxEdits) {
                    break;
                }
            }
        }
        return edits;
    }

    private static String sanitizeReplacement(String replacementSource) {
        if (replacementSource == null) {
            return "";
        }
        String trimmed = replacementSource.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[A-Za-z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private static boolean containsReplacement(
            List<InferenceRepairEdit> edits, String replacementSource) {
        for (InferenceRepairEdit edit : edits) {
            if (edit.getReplacementSource().equals(replacementSource)) {
                return true;
            }
        }
        return false;
    }
}
