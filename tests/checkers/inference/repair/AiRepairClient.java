package checkers.inference.repair;

import java.util.List;

/** External AI repair proposer used by {@link AiRepairEditProvider}. */
public interface AiRepairClient {
    List<String> proposeReplacementSources(RepairPromptContext context);
}
