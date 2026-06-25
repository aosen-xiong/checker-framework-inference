package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SourceRepairPlanSelectorTest {
    @Test
    public void selectsLowestCostNonIdentityEditsFromSourceRealizableUnits() {
        SourceRepairUnit unit =
                new SourceRepairUnit(
                        "unit-a",
                        "ANNOTATION_SITE",
                        "INFERRED_ANNOTATION_SLOT",
                        true,
                        "loc",
                        "evidence",
                        Arrays.asList(
                                new SourceRepairEditOption("Identity", "", 0),
                                new SourceRepairEditOption("ReplaceQualifier", "@Nullable", 2),
                                new SourceRepairEditOption("InsertQualifier", "@Nullable", 1)));

        List<SourceRepairPlan> plans =
                new SourceRepairPlanSelector().selectTopK(Collections.singletonList(unit), 2);

        assertEquals(2, plans.size());
        assertEquals(1, plans.get(0).getRank());
        assertEquals(1, plans.get(0).getTotalCost());
        assertEquals("unit-a", plans.get(0).getSteps().get(0).getRepairUnitId());
        assertEquals("InsertQualifier", plans.get(0).getSteps().get(0).getEditKind());
        assertEquals(2, plans.get(1).getTotalCost());
        assertEquals("ReplaceQualifier", plans.get(1).getSteps().get(0).getEditKind());
    }

    @Test
    public void ignoresNonRealizableUnits() {
        SourceRepairUnit logicalUnit =
                new SourceRepairUnit(
                        "logical",
                        "LOGICAL_CONSTRAINT",
                        "TYPE_SYSTEM_OR_DECLARED_QUALIFIER",
                        false,
                        "loc",
                        "evidence",
                        Arrays.asList(
                                new SourceRepairEditOption("Identity", "", 0),
                                new SourceRepairEditOption("RemoveQualifier", "", 3)));

        List<SourceRepairPlan> plans =
                new SourceRepairPlanSelector().selectTopK(Collections.singletonList(logicalUnit), 5);

        assertEquals(0, plans.size());
    }
}
