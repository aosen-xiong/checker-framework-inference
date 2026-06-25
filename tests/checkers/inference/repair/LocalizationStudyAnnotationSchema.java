package checkers.inference.repair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared labels for manual annotation of localization-study cases. */
public final class LocalizationStudyAnnotationSchema {
    private LocalizationStudyAnnotationSchema() {}

    public static final List<String> REPAIR_SCHEMAS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "ChangeQualifier",
                            "InsertNullGuard",
                            "InsertRequireNonNull",
                            "InsertDefaultValue",
                            "InitializeField",
                            "PropagateNullable",
                            "ChangeReturnContract",
                            "ChangeParameterContract",
                            "AddPrecondition",
                            "AddPostcondition",
                            "SuppressOrCast",
                            "NoValidRepair",
                            "Unknown"));

    public static final List<String> REPAIR_SCOPE_LABELS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "single-site",
                            "multi-site",
                            "intra-method",
                            "inter-method",
                            "public-api-change",
                            "library-or-stub-change",
                            "semantic-hole-needed"));

    public static final List<String> HIT_LABELS =
            Collections.unmodifiableList(Arrays.asList("yes", "partial", "no", "unclear"));
}
