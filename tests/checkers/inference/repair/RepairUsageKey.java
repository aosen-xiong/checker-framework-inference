package checkers.inference.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Source-search key derived from a repair target. */
public final class RepairUsageKey {
    private final String displayName;
    private final String searchText;
    private final boolean identifier;

    private RepairUsageKey(String displayName, String searchText, boolean identifier) {
        this.displayName = displayName;
        this.searchText = searchText;
        this.identifier = identifier;
    }

    public static List<RepairUsageKey> fromTarget(InferenceRepairTarget target) {
        String originalText = target.getOriginalText() == null ? "" : target.getOriginalText().trim();
        if (originalText.isEmpty()) {
            return Collections.emptyList();
        }

        List<RepairUsageKey> keys = new ArrayList<>();
        if (isJavaIdentifier(originalText)) {
            keys.add(new RepairUsageKey(originalText, originalText, true));
            return keys;
        }

        keys.add(new RepairUsageKey(originalText, originalText, false));
        String methodName = methodName(originalText);
        if (!methodName.isEmpty()) {
            addIfMissing(keys, new RepairUsageKey(methodName + "()", methodName + "(", false));
        }
        String receiver = receiver(originalText);
        if (!receiver.isEmpty()) {
            addIfMissing(keys, new RepairUsageKey(receiver, receiver, isJavaIdentifier(receiver)));
        }
        return keys;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSearchText() {
        return searchText;
    }

    public boolean requiresIdentifierBoundary() {
        return identifier;
    }

    private static void addIfMissing(List<RepairUsageKey> keys, RepairUsageKey candidate) {
        for (RepairUsageKey key : keys) {
            if (key.getSearchText().equals(candidate.getSearchText())) {
                return;
            }
        }
        keys.add(candidate);
    }

    private static String methodName(String expression) {
        int paren = expression.indexOf('(');
        if (paren < 0) {
            return "";
        }
        int end = paren - 1;
        while (end >= 0 && Character.isWhitespace(expression.charAt(end))) {
            end--;
        }
        int start = end;
        while (start >= 0 && Character.isJavaIdentifierPart(expression.charAt(start))) {
            start--;
        }
        String name = expression.substring(start + 1, end + 1);
        return isJavaIdentifier(name) ? name : "";
    }

    private static String receiver(String expression) {
        int dot = expression.lastIndexOf('.');
        if (dot <= 0) {
            return "";
        }
        String receiver = expression.substring(0, dot).trim();
        return receiver.indexOf('(') >= 0 ? "" : receiver;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
