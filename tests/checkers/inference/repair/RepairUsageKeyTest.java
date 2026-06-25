package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

public class RepairUsageKeyTest {
    @Test
    public void createsIdentifierKey() {
        List<RepairUsageKey> keys = RepairUsageKey.fromTarget(target("maybeId", "IDENTIFIER"));

        assertEquals(1, keys.size());
        assertEquals("maybeId", keys.get(0).getSearchText());
        assertTrue(keys.get(0).requiresIdentifierBoundary());
    }

    @Test
    public void createsExpressionMethodAndReceiverKeysForMethodInvocation() {
        List<RepairUsageKey> keys =
                RepairUsageKey.fromTarget(target("user.getMapView()", "METHOD_INVOCATION"));

        assertEquals(3, keys.size());
        assertEquals("user.getMapView()", keys.get(0).getSearchText());
        assertFalse(keys.get(0).requiresIdentifierBoundary());
        assertEquals("getMapView(", keys.get(1).getSearchText());
        assertEquals("user", keys.get(2).getSearchText());
        assertTrue(keys.get(2).requiresIdentifierBoundary());
    }

    private static InferenceRepairTarget target(String originalText, String treeKind) {
        return new InferenceRepairTarget(
                new File("Test.java"),
                treeKind,
                0,
                originalText.length(),
                1,
                1,
                originalText);
    }
}
