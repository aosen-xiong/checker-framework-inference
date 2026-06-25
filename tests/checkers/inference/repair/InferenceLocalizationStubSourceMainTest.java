package checkers.inference.repair;

import static org.junit.Assert.assertTrue;

import checkers.inference.test.InferenceTestUtilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class InferenceLocalizationStubSourceMainTest {
    @Test
    public void generatesMissingClassStubFromSourcePackage() throws Exception {
        File root = new File("build/inference-localization-stubs/source-package/project");
        File source = new File(root, "src/main/java/example/A.java");
        write(
                source,
                "package example;\n"
                        + "class A {\n"
                        + "  MissingType value;\n"
                        + "}\n");
        File csv = new File("build/inference-localization-stubs/source-package/input.csv");
        File outputDirectory = new File("build/inference-localization-stubs/source-package/stubs");
        File sourceList = new File("build/inference-localization-stubs/source-package/stubs.txt");
        write(
                csv,
                "projectName,sourceFile,runError,solverHadSolution\n"
                        + "\"fixture\",\""
                        + source.getPath()
                        + "\",\"javac failed during inference: "
                        + source.getPath()
                        + ":2: error: cannot find symbol\\n"
                        + "  symbol:   class MissingType\\n\",false\n");

        InferenceLocalizationStubSourceMain.main(
                new String[] {
                    "--csv",
                    csv.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--source-list-out",
                    sourceList.getPath()
                });

        File stub = new File(outputDirectory, "example/MissingType.java");
        assertTrue(stub.isFile());
        assertTrue(String.join("\n", InferenceTestUtilities.getLines(stub)).contains("package example;"));
        assertTrue(String.join("\n", InferenceTestUtilities.getLines(stub)).contains("public class MissingType"));
        assertTrue(InferenceTestUtilities.getLines(sourceList).contains(stub.getAbsolutePath()));
    }

    @Test
    public void prefersImportedPackageFromDiagnosticSource() throws Exception {
        File root = new File("build/inference-localization-stubs/imported-package/project");
        File source = new File(root, "src/main/java/example/A.java");
        write(source, "package example;\nclass A {}\n");
        File diagnosticSource = new File(root, "com/acme/UseDependency.java");
        write(
                diagnosticSource,
                "package com.acme;\n"
                        + "import dep.external.ExternalType;\n"
                        + "class UseDependency {\n"
                        + "  ExternalType value;\n"
                        + "}\n");
        File csv = new File("build/inference-localization-stubs/imported-package/input.csv");
        File outputDirectory = new File("build/inference-localization-stubs/imported-package/stubs");
        write(
                csv,
                "projectName,sourceFile,runError,solverHadSolution\n"
                        + "\"fixture\",\""
                        + source.getPath()
                        + "\",\"javac failed during inference: /java/com/acme/UseDependency.java:3: "
                        + "error: cannot find symbol\\n"
                        + "  symbol:   class ExternalType\\n\",false\n");

        InferenceLocalizationStubSourceMain.main(
                new String[] {
                    "--csv",
                    csv.getPath(),
                    "--out-dir",
                    outputDirectory.getPath(),
                    "--project-root",
                    root.getPath()
                });

        File stub = new File(outputDirectory, "dep/external/ExternalType.java");
        assertTrue(stub.isFile());
        List<String> lines = InferenceTestUtilities.getLines(stub);
        assertTrue(String.join("\n", lines).contains("package dep.external;"));
        assertTrue(String.join("\n", lines).contains("public class ExternalType"));
    }

    @Test
    public void enrichesGeneratedStubsWithMissingMembers() throws Exception {
        File root = new File("build/inference-localization-stubs/members/project");
        File source = new File(root, "src/main/java/example/A.java");
        write(source, "package example;\nclass A { MissingType value; }\n");
        File csv = new File("build/inference-localization-stubs/members/input.csv");
        File outputDirectory = new File("build/inference-localization-stubs/members/stubs");
        write(
                csv,
                "projectName,sourceFile,runError,solverHadSolution\n"
                        + "\"fixture\",\""
                        + source.getPath()
                        + "\",\"javac failed during inference: "
                        + source.getPath()
                        + ":2: error: cannot find symbol\\n"
                        + "  symbol:   class MissingType\\n"
                        + source.getPath()
                        + ":3: error: cannot find symbol\\n"
                        + "    value.getName();\\n"
                        + "         ^\\n"
                        + "  symbol:   method getName()\\n"
                        + "  location: variable value of type MissingType\\n"
                        + source.getPath()
                        + ":4: error: cannot find symbol\\n"
                        + "    MissingType.Kind.PRIMARY.name();\\n"
                        + "               ^\\n"
                        + "  symbol:   variable Kind\\n"
                        + "  location: class MissingType\\n"
                        + source.getPath()
                        + ":5: error: cannot find symbol\\n"
                        + "    MissingType.Kind.Application.name();\\n"
                        + "               ^\\n"
                        + "  symbol:   variable Kind\\n"
                        + "  location: class MissingType\\n"
                        + source.getPath()
                        + ":6: error: cannot find symbol\\n"
                        + "  symbol:   method hasRegions()\\n"
                        + "  location: variable value of type MissingType\\n"
                        + source.getPath()
                        + ":7: error: cannot find symbol\\n"
                        + "  symbol:   method getInstances()\\n"
                        + "  location: variable value of type MissingType\\n\",false\n");

        InferenceLocalizationStubSourceMain.main(
                new String[] {
                    "--csv",
                    csv.getPath(),
                    "--out-dir",
                    outputDirectory.getPath()
                });

        File stub = new File(outputDirectory, "example/MissingType.java");
        assertTrue(stub.isFile());
        String generated = String.join("\n", InferenceTestUtilities.getLines(stub));
        assertTrue(generated.contains("public MissingType(Object... ignored)"));
        assertTrue(generated.contains("public enum Kind { PRIMARY, Application }"));
        assertTrue(generated.contains("public <T> T getName() { return null; }"));
        assertTrue(generated.contains("public boolean hasRegions() { return false; }"));
        assertTrue(
                generated.contains(
                        "public <T> java.util.List<T> getInstances() { return java.util.Collections.emptyList(); }"));
    }

    @Test
    public void mergesStubInformationFromMultipleCsvFiles() throws Exception {
        File root = new File("build/inference-localization-stubs/multi-csv/project");
        File source = new File(root, "src/main/java/example/A.java");
        write(source, "package example;\nclass A { MissingType value; }\n");
        File typeCsv = new File("build/inference-localization-stubs/multi-csv/types.csv");
        File memberCsv = new File("build/inference-localization-stubs/multi-csv/members.csv");
        File outputDirectory = new File("build/inference-localization-stubs/multi-csv/stubs");
        write(
                typeCsv,
                "projectName,sourceFile,runError,solverHadSolution\n"
                        + "\"fixture\",\""
                        + source.getPath()
                        + "\",\"javac failed during inference: "
                        + source.getPath()
                        + ":2: error: cannot find symbol\\n"
                        + "  symbol:   class MissingType\\n\",false\n");
        write(
                memberCsv,
                "projectName,sourceFile,runError,solverHadSolution\n"
                        + "\"fixture\",\""
                        + source.getPath()
                        + "\",\"javac failed during inference: "
                        + source.getPath()
                        + ":3: error: cannot find symbol\\n"
                        + "  symbol:   method getName()\\n"
                        + "  location: variable value of type MissingType\\n\",false\n");

        InferenceLocalizationStubSourceMain.main(
                new String[] {
                    "--csv",
                    typeCsv.getPath(),
                    "--csv",
                    memberCsv.getPath(),
                    "--out-dir",
                    outputDirectory.getPath()
                });

        File stub = new File(outputDirectory, "example/MissingType.java");
        assertTrue(stub.isFile());
        String generated = String.join("\n", InferenceTestUtilities.getLines(stub));
        assertTrue(generated.contains("public class MissingType"));
        assertTrue(generated.contains("public <T> T getName() { return null; }"));
    }

    private static void write(File file, String contents) throws Exception {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
