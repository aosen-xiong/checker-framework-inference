package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A source-level repair unit derived from inference constraint provenance. */
public final class SourceRepairUnit {
    private final String id;
    private final String kind;
    private final String provenanceKind;
    private final boolean sourceRealizable;
    private final String location;
    private final String evidence;
    private final List<SourceRepairEditOption> editDomain;

    public SourceRepairUnit(
            String id,
            String kind,
            String provenanceKind,
            boolean sourceRealizable,
            String location,
            String evidence,
            List<SourceRepairEditOption> editDomain) {
        this.id = id;
        this.kind = kind;
        this.provenanceKind = provenanceKind;
        this.sourceRealizable = sourceRealizable;
        this.location = location;
        this.evidence = evidence;
        this.editDomain = Collections.unmodifiableList(new ArrayList<>(editDomain));
    }

    public String getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getProvenanceKind() {
        return provenanceKind;
    }

    public boolean isSourceRealizable() {
        return sourceRealizable;
    }

    public String getLocation() {
        return location;
    }

    public String getEvidence() {
        return evidence;
    }

    public List<SourceRepairEditOption> getEditDomain() {
        return editDomain;
    }
}
