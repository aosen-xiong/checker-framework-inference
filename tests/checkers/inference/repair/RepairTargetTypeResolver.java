package checkers.inference.repair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-based expected-type resolver for repair targets. */
public final class RepairTargetTypeResolver {
    private static final Pattern METHOD_DECLARATION_PATTERN =
            Pattern.compile(
                    "(?s)(?:public|protected|private|static|final|synchronized|native|abstract|\\s)*"
                            + "[A-Za-z_$][A-Za-z0-9_$.<>?,\\[\\] ]*\\s+"
                            + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)");
    private static final Pattern FIELD_DECLARATION_PATTERN =
            Pattern.compile(
                    "(?m)^[ \\t]*(?:@NonNull\\s+)?([A-Za-z_$][A-Za-z0-9_$.<>?, ]*)\\s+"
                            + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=|;)");

    private RepairTargetTypeResolver() {}

    public static String expectedType(InferenceRepairTarget target, String originalSource) {
        String type = expectedTypeFromAssignment(target, originalSource);
        if (type != null) {
            return type;
        }
        return expectedTypeFromMethodCall(target, originalSource);
    }

    public static String normalizeType(String type) {
        return type.replaceAll("@[A-Za-z_$][A-Za-z0-9_$.]*(?:\\([^)]*\\))?\\s*", "").trim();
    }

    private static String expectedTypeFromAssignment(
            InferenceRepairTarget target, String originalSource) {
        int statementStart = statementStart(target, originalSource);
        int statementEnd = statementEnd(target, originalSource);
        if (statementStart < 0 || statementEnd <= statementStart) {
            return null;
        }
        String statement = originalSource.substring(statementStart, statementEnd);
        int equals = statement.indexOf('=');
        if (equals < 0) {
            return null;
        }
        String lhs = statement.substring(0, equals).trim();
        if (lhs.contains(" ")) {
            return normalizeType(lhs.substring(0, lhs.lastIndexOf(' ')));
        }
        return declaredType(lhs.substring(lhs.lastIndexOf('.') + 1), originalSource);
    }

    private static String expectedTypeFromMethodCall(
            InferenceRepairTarget target, String originalSource) {
        int targetStart = checkedOffset(target.getStartOffset());
        int openParen = originalSource.lastIndexOf('(', targetStart);
        if (openParen < 0) {
            return null;
        }
        String methodName = methodNameBefore(originalSource, openParen);
        if (methodName == null) {
            return null;
        }
        int argumentIndex = argumentIndex(originalSource, openParen, targetStart);
        Matcher matcher = METHOD_DECLARATION_PATTERN.matcher(originalSource);
        while (matcher.find()) {
            if (methodName.equals(matcher.group(1))) {
                String type = parameterType(matcher.group(2), argumentIndex);
                if (type != null) {
                    return type;
                }
            }
        }
        return null;
    }

    private static String declaredType(String variableName, String originalSource) {
        Matcher matcher = FIELD_DECLARATION_PATTERN.matcher(originalSource);
        while (matcher.find()) {
            if (variableName.equals(matcher.group(2))) {
                return normalizeType(matcher.group(1));
            }
        }
        return null;
    }

    private static String parameterType(String parameterList, int argumentIndex) {
        String[] parameters = parameterList.split(",");
        if (argumentIndex >= parameters.length) {
            return null;
        }
        String parameter = parameters[argumentIndex].trim();
        if (parameter.isEmpty() || !parameter.contains(" ")) {
            return null;
        }
        return normalizeType(parameter.substring(0, parameter.lastIndexOf(' ')));
    }

    private static int argumentIndex(String source, int openParen, int targetStart) {
        int argumentIndex = 0;
        for (int i = openParen + 1; i < targetStart; i++) {
            if (source.charAt(i) == ',') {
                argumentIndex++;
            }
        }
        return argumentIndex;
    }

    private static String methodNameBefore(String source, int openParen) {
        int end = openParen - 1;
        while (end >= 0 && Character.isWhitespace(source.charAt(end))) {
            end--;
        }
        int start = end;
        while (start >= 0 && Character.isJavaIdentifierPart(source.charAt(start))) {
            start--;
        }
        if (start == end) {
            return null;
        }
        return source.substring(start + 1, end + 1);
    }

    private static int statementStart(InferenceRepairTarget target, String source) {
        int targetStart = checkedOffset(target.getStartOffset());
        int previousSemicolon = source.lastIndexOf(';', targetStart);
        int previousOpenBrace = source.lastIndexOf('{', targetStart);
        return Math.max(previousSemicolon, previousOpenBrace) + 1;
    }

    private static int statementEnd(InferenceRepairTarget target, String source) {
        int targetStart = checkedOffset(target.getStartOffset());
        return source.indexOf(';', targetStart) + 1;
    }

    private static int checkedOffset(long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid source offset: " + offset);
        }
        return (int) offset;
    }
}
