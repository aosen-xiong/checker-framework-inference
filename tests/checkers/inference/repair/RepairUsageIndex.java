package checkers.inference.repair;

import java.util.List;

/** Index for project-local usages relevant to one repair target. */
public interface RepairUsageIndex {
    List<RepairUsageExample> findExamples(RepairUsageQuery query);
}
