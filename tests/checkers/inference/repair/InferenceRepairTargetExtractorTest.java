package checkers.inference.repair;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class InferenceRepairTargetExtractorTest {
    private final InferenceRepairTargetExtractor extractor = new InferenceRepairTargetExtractor();

    @Test
    public void resolvesLocalVariableTarget() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/InferenceUnsatAssignment.java"),
                        candidateWithLocation(
                                "AstPathLocation( InferenceUnsatAssignment.setId(Ljava/lang/String;)V.null:"
                                        + "InferenceUnsatAssignment:setId(Ljava/lang/String;)V::"
                                        + "Method.body, Block.statement 0 )"));

        assertEquals("VARIABLE", target.getTreeKind());
        assertEquals("@NonNull String id = maybeId;", target.getOriginalText());
    }

    @Test
    public void resolvesAssignmentExpressionTarget() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/AssignmentRepair.java"),
                        candidateWithLocation(
                                "AstPathLocation( AssignmentRepair.setId(Ljava/lang/String;)V.null:"
                                        + "AssignmentRepair:setId(Ljava/lang/String;)V::Method.body,"
                                        + " Block.statement 0, ExpressionStatement.expression,"
                                        + " Assignment.expression )"));

        assertEquals("IDENTIFIER", target.getTreeKind());
        assertEquals("id", target.getOriginalText());
    }

    @Test
    public void resolvesAssignmentExpressionTargetWhenPathStopsAtAssignment() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/InferenceUnsatFieldAssignment.java"),
                        candidateWithLocation(
                                "AstPathLocation( InferenceUnsatFieldAssignment.setId(Ljava/lang/String;)V.null:"
                                        + "InferenceUnsatFieldAssignment:setId(Ljava/lang/String;)V::"
                                        + "Method.body, Block.statement 0,"
                                        + " ExpressionStatement.expression )"));

        assertEquals("IDENTIFIER", target.getTreeKind());
        assertEquals("maybeId", target.getOriginalText());
    }

    @Test
    public void resolvesMethodInvocationSelectTarget() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/MethodCallRepair.java"),
                        candidateWithLocation(
                                "AstPathLocation( MethodCallRepair.setId(Ljava/lang/String;)V.null:"
                                        + "MethodCallRepair:setId(Ljava/lang/String;)V::Method.body,"
                                        + " Block.statement 0, ExpressionStatement.expression,"
                                        + " MethodInvocation.methodSelect )"));

        assertEquals("IDENTIFIER", target.getTreeKind());
        assertEquals("recordId", target.getOriginalText());
    }

    @Test
    public void resolvesMethodInvocationArgumentTarget() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/InferenceUnsatMethodCall.java"),
                        candidateWithLocation(
                                "AstPathLocation( InferenceUnsatMethodCall.setId(Ljava/lang/String;)V.null:"
                                        + "InferenceUnsatMethodCall:setId(Ljava/lang/String;)V::"
                                        + "Method.body, Block.statement 1,"
                                        + " ExpressionStatement.expression,"
                                        + " MethodInvocation.argument 0 )"));

        assertEquals("IDENTIFIER", target.getTreeKind());
        assertEquals("maybeId", target.getOriginalText());
    }

    @Test
    public void resolvesSingleArgumentWhenPathStopsAtMethodInvocation() {
        InferenceRepairTarget target =
                extractor.extract(
                        new File("testdata/repair/InferenceUnsatMethodCall.java"),
                        candidateWithLocation(
                                "AstPathLocation( InferenceUnsatMethodCall.setId(Ljava/lang/String;)V.null:"
                                        + "InferenceUnsatMethodCall:setId(Ljava/lang/String;)V::"
                                        + "Method.body, Block.statement 1,"
                                        + " ExpressionStatement.expression )"));

        assertEquals("IDENTIFIER", target.getTreeKind());
        assertEquals("maybeId", target.getOriginalText());
    }

    private static InferenceRepairCandidate candidateWithLocation(String location) {
        return new InferenceRepairCandidate(
                null,
                new InferenceSlotContext(1, "VARIABLE", false, "AST_PATH", location, "slot#1"),
                InferenceRepairKind.INSERT_NULL_GUARD,
                "@Nullable",
                "test candidate");
    }
}
