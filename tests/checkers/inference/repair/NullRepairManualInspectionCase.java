package checkers.inference.repair;

/** One row from NullRepair's manual-inspection scoring table. */
public final class NullRepairManualInspectionCase {
    private final String benchmark;
    private final String id;
    private final String type;
    private final String message;
    private final String path;
    private final String expression;
    private final int scoreA;
    private final int scoreB;
    private final int scoreC;

    NullRepairManualInspectionCase(
            String benchmark,
            String id,
            String type,
            String message,
            String path,
            String expression,
            int scoreA,
            int scoreB,
            int scoreC) {
        this.benchmark = benchmark;
        this.id = id;
        this.type = type;
        this.message = message;
        this.path = path;
        this.expression = expression;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.scoreC = scoreC;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getExpression() {
        return expression;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public int getScoreC() {
        return scoreC;
    }

    public int bestScore() {
        return Math.min(scoreA, Math.min(scoreB, scoreC));
    }

    /** Returns the source path relative to this case's benchmark root, without the trailing line. */
    public String sourcePathRelativeToBenchmarkRoot() {
        String sourcePath = stripLineNumber(path);
        String prefix = benchmark + "/";
        return sourcePath.startsWith(prefix) ? sourcePath.substring(prefix.length()) : sourcePath;
    }

    public int lineNumber() {
        int colon = path.lastIndexOf(':');
        if (colon < 0 || colon == path.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(path.substring(colon + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String stripLineNumber(String value) {
        int colon = value.lastIndexOf(':');
        if (colon < 0 || colon == value.length() - 1) {
            return value;
        }
        for (int i = colon + 1; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return value;
            }
        }
        return value.substring(0, colon);
    }
}
