package checkers.inference.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Query used to retrieve project-local usage examples for one repair target. */
public final class RepairUsageQuery {
    private final InferenceRepairCandidate candidate;
    private final InferenceRepairTarget target;
    private final String originalSource;
    private final List<File> projectSourceFiles;

    public RepairUsageQuery(
            InferenceRepairCandidate candidate,
            InferenceRepairTarget target,
            String originalSource,
            List<File> projectSourceFiles) {
        this.candidate = candidate;
        this.target = target;
        this.originalSource = originalSource;
        this.projectSourceFiles = Collections.unmodifiableList(new ArrayList<>(projectSourceFiles));
    }

    public InferenceRepairCandidate getCandidate() {
        return candidate;
    }

    public InferenceRepairTarget getTarget() {
        return target;
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public List<File> getProjectSourceFiles() {
        return projectSourceFiles;
    }
}
