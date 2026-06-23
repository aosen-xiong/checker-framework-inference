package checkers.inference.test;

import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.checkerframework.framework.test.diagnostics.JavaDiagnosticReader;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** Runs one checker and exposes its diagnostics as structured test objects. */
public final class CheckerDiagnosticCapture {
    private static final String DEFAULT_TEST_SOURCE_PATH = "testdata";

    private CheckerDiagnosticCapture() {}

    public static Result run(Class<?> checker, File sourceFile, List<String> javacOptions) {
        return run(checker, Collections.singletonList(sourceFile), javacOptions);
    }

    public static Result run(
            Class<?> checker, Iterable<File> sourceFiles, List<String> javacOptions) {
        List<File> sourceFileList = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            sourceFileList.add(sourceFile);
        }

        List<String> javacOptionList = new ArrayList<>(javacOptions);
        TestConfiguration configuration =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        DEFAULT_TEST_SOURCE_PATH,
                        sourceFileList,
                        Collections.singletonList(checker.getCanonicalName()),
                        javacOptionList,
                        false);

        List<TestDiagnostic> expectedDiagnostics =
                JavaDiagnosticReader.readJavaSourceFiles(configuration.getTestSourceFiles());
        CompilationResult compilationResult = new TypecheckExecutor().compile(configuration);
        TypecheckResult typecheckResult =
                TypecheckResult.fromCompilationResults(
                        configuration, compilationResult, expectedDiagnostics);

        return new Result(
                checker,
                sourceFileList,
                javacOptionList,
                configuration,
                compilationResult,
                expectedDiagnostics,
                typecheckResult);
    }

    public static final class Result {
        private final Class<?> checker;
        private final List<File> sourceFiles;
        private final List<String> javacOptions;
        private final TestConfiguration configuration;
        private final CompilationResult compilationResult;
        private final List<TestDiagnostic> expectedDiagnostics;
        private final TypecheckResult typecheckResult;

        private Result(
                Class<?> checker,
                List<File> sourceFiles,
                List<String> javacOptions,
                TestConfiguration configuration,
                CompilationResult compilationResult,
                List<TestDiagnostic> expectedDiagnostics,
                TypecheckResult typecheckResult) {
            this.checker = checker;
            this.sourceFiles = Collections.unmodifiableList(new ArrayList<>(sourceFiles));
            this.javacOptions = Collections.unmodifiableList(new ArrayList<>(javacOptions));
            this.configuration = configuration;
            this.compilationResult = compilationResult;
            this.expectedDiagnostics =
                    Collections.unmodifiableList(new ArrayList<>(expectedDiagnostics));
            this.typecheckResult = typecheckResult;
        }

        public Class<?> getChecker() {
            return checker;
        }

        public List<File> getSourceFiles() {
            return sourceFiles;
        }

        public List<String> getJavacOptions() {
            return javacOptions;
        }

        public TestConfiguration getConfiguration() {
            return configuration;
        }

        public CompilationResult getCompilationResult() {
            return compilationResult;
        }

        public List<Diagnostic<? extends JavaFileObject>> getActualDiagnostics() {
            return typecheckResult.getActualDiagnostics();
        }

        public List<TestDiagnostic> getExpectedDiagnostics() {
            return expectedDiagnostics;
        }

        public TypecheckResult getTypecheckResult() {
            return typecheckResult;
        }

        public boolean didTestFail() {
            return typecheckResult.didTestFail();
        }

        public String summarize() {
            return typecheckResult.summarize();
        }
    }
}
