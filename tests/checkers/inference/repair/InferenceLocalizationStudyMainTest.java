package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class InferenceLocalizationStudyMainTest {
    @Test
    public void writesLocalizationStudyReportWithManualAnnotationFields() {
        File outputFile = new File("build/inference-localization-study-main/report.json");
        File csvOutputFile = new File("build/inference-localization-study-main/report.csv");

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--csv-out",
                    csvOutputFile.getPath(),
                    "--sample-size",
                    "1",
                    "--seed",
                    "7",
                    "--top-k",
                    "3",
                    "--source-filter",
                    "NULLNESS_RELEVANT",
                    "--project-name",
                    "fixtures",
                    "--source",
                    "testdata/repair/InferenceUnsatMethodCall.java"
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("\"projectName\":\"fixtures\""));
        assertTrue(json.contains("\"sampledInputCount\":1"));
        assertTrue(json.contains("\"sourceFilter\":\"NULLNESS_RELEVANT\""));
        assertTrue(json.contains("\"topK\":3"));
        assertTrue(json.contains("solver-backed retained-constraint oracle"));
        assertTrue(json.contains("\"allowedRepairSchemas\""));
        assertTrue(json.contains("\"InsertNullGuard\""));
        assertTrue(json.contains("\"allowedRepairScopeLabels\""));
        assertTrue(json.contains("\"semantic-hole-needed\""));
        assertTrue(json.contains("\"allowedHitLabels\""));
        assertTrue(json.contains("\"sourceFile\":\"testdata/repair/InferenceUnsatMethodCall.java\""));
        assertTrue(json.contains("\"mcsOracleKind\":\"SOLVER_BACKED\""));
        assertTrue(json.contains("\"mcsOriginalUniverseSize\":"));
        assertTrue(json.contains("\"mcsEnumeratedUniverseSize\":"));
        assertTrue(json.contains("\"mcsUniverseTruncated\":false"));
        assertTrue(json.contains("\"mcsMaxRemovalSize\":"));
        assertTrue(json.contains("\"mcsSearchBounded\":"));
        assertTrue(json.contains("\"sourceRepairUnitCount\":"));
        assertTrue(json.contains("\"sourceRealizableRepairUnitCount\":"));
        assertTrue(json.contains("\"hasSourceRealizableRepairUnit\":true"));
        assertTrue(json.contains("\"unsatCoreLocations\""));
        assertTrue(json.contains("\"sourceRepairUnits\""));
        assertTrue(json.contains("\"sourceRealizable\":true"));
        assertTrue(json.contains("\"kind\":\"ANNOTATION_SITE\""));
        assertTrue(json.contains("\"provenanceKind\":\"INFERRED_ANNOTATION_SLOT\""));
        assertTrue(json.contains("\"editDomain\""));
        assertTrue(json.contains("\"kind\":\"InsertQualifier\""));
        assertTrue(json.contains("\"sourceRepairPlans\""));
        assertTrue(json.contains("\"totalCost\":1"));
        assertTrue(json.contains("\"repairUnitId\""));
        assertTrue(json.contains("\"editKind\":\"InsertQualifier\""));
        assertTrue(json.contains("\"sourceRepairPlanValidation\":null"));
        assertTrue(json.contains("\"weightedMcsCandidates\""));
        assertTrue(json.contains("\"rank\":1"));
        assertTrue(json.contains("\"locations\""));
        assertTrue(json.contains("\"evidence\""));
        assertTrue(json.contains("\"humanRepairLocations\":[]"));
        assertTrue(json.contains("\"humanRepairUnitIds\":[]"));
        assertTrue(json.contains("\"humanRepairSchemas\":[]"));
        assertTrue(json.contains("\"top1Hit\":null"));
        assertTrue(json.contains("\"top3Hit\":null"));
        assertTrue(json.contains("\"top5Hit\":null"));
        assertTrue(json.contains("\"sourceRealizableTop1Hit\":null"));
        assertTrue(json.contains("\"sourceRealizableTop3Hit\":null"));
        assertTrue(json.contains("\"sourceRealizableTop5Hit\":null"));

        String csv = String.join("\n", InferenceTestUtilities.getLines(csvOutputFile));
        assertTrue(csv.contains("projectName,revision,sourceFile"));
        assertTrue(csv.contains("mcsMaxRemovalSize,mcsSearchBounded"));
        assertTrue(csv.contains("sourceRepairUnitCount,sourceRealizableRepairUnitCount"));
        assertTrue(
                csv.contains(
                        "top1Weight,top1SourceRepairUnitCount,"
                                + "top1SourceRealizableRepairUnitCount,"
                                + "top1HasSourceRealizableRepairUnit,top1Locations,top1Evidence"));
        assertTrue(
                csv.contains(
                        "humanRepairLocations,humanRepairUnitIds,humanRepairSchemas,"
                                + "humanRepairNotes,top1Hit,top3Hit,top5Hit,"
                                + "sourceRealizableTop1Hit,sourceRealizableTop3Hit,"
                                + "sourceRealizableTop5Hit,"
                                + "sourceRepairPlanMaterialized,"
                                + "sourceRepairPlanInferenceSolved,"
                                + "sourceRepairPlanVerified,"
                                + "sourceRepairFollowUpAttempted,"
                                + "sourceRepairFollowUpVerified,"
                                + "sourceRepairFollowUpAppliedEdit,"
                                + "sourceRepairFollowUpReplacementSource,"
                                + "sourceRepairPlanValidationError"));
        assertTrue(csv.contains("\"fixtures\""));
        assertTrue(csv.contains("\"testdata/repair/InferenceUnsatMethodCall.java\""));
        assertTrue(csv.contains("\"SOLVER_BACKED\""));
    }

    @Test
    public void resolvesSourceListEntriesAgainstProjectRootRegardlessOfArgumentOrder()
            throws Exception {
        File sourceList = new File("build/inference-localization-study-main/sources.txt");
        File outputFile = new File("build/inference-localization-study-main/source-list-report.json");
        File parent = sourceList.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                sourceList.toPath(),
                "testdata/repair/InferenceUnsatAssignment.java\n".getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--source-list",
                    sourceList.getPath(),
                    "--project-root",
                    ".",
                    "--sample-size",
                    "1"
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("\"projectRoot\":\".\""));
        assertTrue(json.contains("\"sourceFile\":\"./testdata/repair/InferenceUnsatAssignment.java\""));
    }

    @Test
    public void recordsJavacDiagnosticsForRunErrors() throws Exception {
        File sourceFile = new File("build/inference-localization-study-main/bad/BadSource.java");
        File outputFile = new File("build/inference-localization-study-main/bad/report.json");
        File parent = sourceFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(
                sourceFile.toPath(),
                "class BadSource { MissingType value; }\n".getBytes(StandardCharsets.UTF_8));

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--source",
                    sourceFile.getPath(),
                    "--sample-size",
                    "1",
                    "--in-process"
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("javac failed during inference"));
        assertTrue(json.contains("MissingType"));
    }

    @Test
    public void recordsVariableAnnotatorCrashReproducer() {
        File outputFile =
                new File("build/inference-localization-study-main/variable-annotator-crash.json");

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--source",
                    "testdata/repair/TaskExecutorsNestedCrashRepro.java",
                    "--sample-size",
                    "1",
                    "--in-process"
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("Error in AnnotatedTypeMirror.fromExpression"));
        assertTrue(json.contains("condExpr#num0"));
        assertTrue(json.contains("VariableAnnotator.expensiveBackupGetPath"));
        assertTrue(json.contains("TreePath.getParentPath()"));
    }

    @Test
    public void forwardsJavacOptionsFileToInferenceRun() throws Exception {
        File root = new File("build/inference-localization-study-main/javac-options");
        File dependency = new File(root, "deps/dep/Dep.java");
        File sourceFile = new File(root, "src/UseDep.java");
        File optionsFile = new File(root, "javac-options.txt");
        File outputFile = new File(root, "report.json");
        write(
                dependency,
                "package dep;\n"
                        + "public class Dep {}\n");
        write(
                sourceFile,
                "import dep.Dep;\n"
                        + "class UseDep { Dep dep = new Dep(); }\n");
        write(optionsFile, "-sourcepath\n" + new File(root, "deps").getPath() + "\n");

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--source",
                    sourceFile.getPath(),
                    "--sample-size",
                    "1",
                    "--in-process",
                    "--javac-options-file",
                    optionsFile.getPath()
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("\"sourceFile\":\"" + sourceFile.getPath() + "\""));
        assertTrue(!json.contains("javac failed during inference"));
        assertTrue(!json.contains("package dep does not exist"));
    }

    @Test
    public void canValidateTopSourceRepairPlanWhenRequested() {
        File outputFile =
                new File("build/inference-localization-study-main/validated-report.json");
        File csvOutputFile =
                new File("build/inference-localization-study-main/validated-report.csv");

        InferenceLocalizationStudyMain.main(
                new String[] {
                    "--out",
                    outputFile.getPath(),
                    "--csv-out",
                    csvOutputFile.getPath(),
                    "--sample-size",
                    "1",
                    "--top-k",
                    "1",
                    "--validate-source-repair-plans",
                    "--source",
                    "testdata/repair/InferenceUnsatMethodCall.java"
                });

        String json = String.join("\n", InferenceTestUtilities.getLines(outputFile));
        assertTrue(json.contains("\"sourceRepairPlanValidation\":{"));
        assertTrue(json.contains("\"stages\":["));
        assertTrue(json.contains("\"stage\":\"annotation-materialization\""));
        assertTrue(json.contains("\"stage\":\"inference-rerun\""));
        assertTrue(json.contains("\"stage\":\"post-inference-typecheck\""));
        assertTrue(json.contains("\"stage\":\"diagnostic-source-repair\""));
        assertTrue(json.contains("\"planRank\":1"));
        assertTrue(json.contains("\"materialized\":true"));
        assertTrue(json.contains("\"repairedSourceFile\":"));
        assertTrue(json.contains("\"inferenceSolved\":true"));
        assertTrue(json.contains("\"verified\":true"));
        assertTrue(json.contains("\"followUpRepairAttempted\":true"));
        assertTrue(json.contains("\"followUpRepairVerified\":true"));
        assertTrue(
                json.contains(
                        "\"followUpAppliedEdit\":"
                                + "\"replace nullable dereference with null-safe expression\""));
        assertTrue(json.contains("\"followUpReplacementSource\":null"));

        String csv = String.join("\n", InferenceTestUtilities.getLines(csvOutputFile));
        assertTrue(csv.contains("sourceRepairPlanMaterialized"));
        assertTrue(csv.contains(",true,true,true,true,true,"));
        assertTrue(csv.contains("\"replace nullable dereference with null-safe expression\""));
    }

    private static void write(File file, String contents) throws Exception {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
