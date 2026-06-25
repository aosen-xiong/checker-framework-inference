package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.util.Collections;

import org.junit.Test;

public class SourceRepairPlanMaterializerTest {
    @Test
    public void insertsQualifierAtMethodParameterType() {
        File sourceFile = new File("testdata/repair/InferenceUnsatMethodCall.java");
        SourceRepairPlan plan =
                new SourceRepairPlan(
                        1,
                        1,
                        Collections.singletonList(
                                new SourceRepairPlanStep(
                                        "slot#7:AST_PATH:AstPathLocation( "
                                                + "InferenceUnsatMethodCall.recordId"
                                                + "(Ljava/lang/String;)V.null:"
                                                + "InferenceUnsatMethodCall:recordId"
                                                + "(Ljava/lang/String;)V::Method.parameter 0, "
                                                + "Variable.type )",
                                        "InsertQualifier",
                                        "@nninf.qual.Nullable",
                                        1)));

        SourceRepairPlanMaterialization materialization =
                new SourceRepairPlanMaterializer()
                        .materialize(
                                plan,
                                sourceFile,
                                new File("build/source-repair-plan-materializer-test"));

        assertTrue(materialization.getError(), materialization.isMaterialized());
        String repairedSource =
                String.join("\n", InferenceTestUtilities.getLines(materialization.getSourceFile()));
        assertTrue(repairedSource.contains("void recordId(@nninf.qual.Nullable String id)"));
    }
}
