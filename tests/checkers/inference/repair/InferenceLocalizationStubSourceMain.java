package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates minimal Java source stubs for missing class/interface/enum diagnostics. */
public final class InferenceLocalizationStubSourceMain {
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern DIAGNOSTIC_SOURCE_PATTERN =
            Pattern.compile("(?m)([^\\n:]+\\.java):\\d+: error:");
    private static final Pattern MISSING_METHOD_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*symbol:\\s+method\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\(([^)]*)\\)\\s*$\\n"
                            + "^\\s*location:\\s+(?:variable\\s+\\S+\\s+of type|class)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*$");
    private static final Pattern NESTED_ENUM_USAGE_PATTERN =
            Pattern.compile(
                    "\\b([A-Za-z_$][A-Za-z0-9_$]*)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\b");

    private InferenceLocalizationStubSourceMain() {}

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        List<Map<String, String>> rows = new ArrayList<>();
        for (File inputFile : arguments.inputFiles) {
            rows.addAll(readCsv(inputFile));
        }
        StubIndex index = StubIndex.from(rows, arguments.projectRoot);
        List<File> generated = writeStubs(arguments.outputDirectory, index);
        if (arguments.sourceListOutputFile != null) {
            writeSourceList(arguments.sourceListOutputFile, generated);
        }
        System.out.println(
                "Generated "
                        + generated.size()
                        + " dependency stub source(s) in "
                        + arguments.outputDirectory.getPath());
    }

    private static List<Map<String, String>> readCsv(File inputFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read localization CSV: " + inputFile, e);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Localization CSV is empty: " + inputFile);
        }
        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < headers.size(); index++) {
                row.put(headers.get(index), index < values.size() ? values.get(index) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);
            if (quoted) {
                if (c == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static List<File> writeStubs(File outputDirectory, StubIndex index) {
        List<File> generated = new ArrayList<>();
        for (StubSpec spec : index.specs()) {
            File outputFile =
                    new File(
                            outputDirectory,
                            spec.packageName.replace('.', File.separatorChar)
                                    + File.separator
                                    + spec.simpleName
                                    + ".java");
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create stub directory: " + parent);
            }
            try {
                Files.write(outputFile.toPath(), spec.source().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Could not write stub source: " + outputFile, e);
            }
            generated.add(outputFile);
        }
        return generated;
    }

    private static void writeSourceList(File outputFile, List<File> generated) {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create stub source-list directory: " + parent);
        }
        List<String> lines = new ArrayList<>();
        for (File file : generated) {
            lines.add(file.getAbsolutePath());
        }
        try {
            Files.write(outputFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not write stub source list: " + outputFile, e);
        }
    }

    private static final class StubIndex {
        private final Map<String, StubSpec> specs = new LinkedHashMap<>();

        private static StubIndex from(List<Map<String, String>> rows, File projectRoot) {
            StubIndex index = new StubIndex();
            for (Map<String, String> row : rows) {
                String runError = row.get("runError");
                if (runError == null || runError.isEmpty()) {
                    continue;
                }
                SourceContext context = SourceContext.from(row.get("sourceFile"), runError, projectRoot);
                for (String example : RunErrorClassifier.missingDependencyExamples(runError, 50)) {
                    StubSpec spec = context.stubFor(example);
                    if (spec != null) {
                        index.put(spec);
                    }
                }
                context.addMemberDiagnostics(runError, index);
            }
            return index;
        }

        private List<StubSpec> specs() {
            return new ArrayList<>(specs.values());
        }

        private void put(StubSpec spec) {
            StubSpec existing = specs.get(spec.key());
            if (existing == null) {
                specs.put(spec.key(), spec);
            }
        }

        private StubSpec findBySimpleName(String simpleName) {
            for (StubSpec spec : specs.values()) {
                if (spec.simpleName.equals(simpleName)) {
                    return spec;
                }
            }
            return null;
        }
    }

    private static final class SourceContext {
        private final Set<String> fallbackPackages = new LinkedHashSet<>();
        private final Map<String, String> importedPackages = new LinkedHashMap<>();

        private static SourceContext from(String sourceFileName, String runError, File projectRoot) {
            SourceContext context = new SourceContext();
            List<File> sourceFiles = new ArrayList<>();
            if (sourceFileName != null && !sourceFileName.isEmpty()) {
                sourceFiles.add(new File(sourceFileName));
            }
            for (String diagnosticSource : diagnosticSources(runError)) {
                String packageName = inferPackageFromPath(diagnosticSource);
                if (!packageName.isEmpty()) {
                    context.fallbackPackages.add(packageName);
                }
                File resolved = resolveDiagnosticSource(diagnosticSource, projectRoot);
                if (resolved != null) {
                    sourceFiles.add(resolved);
                }
            }
            for (File sourceFile : sourceFiles) {
                SourceInfo info = SourceInfo.read(sourceFile);
                if (info == null) {
                    continue;
                }
                if (!info.packageName.isEmpty()) {
                    context.fallbackPackages.add(info.packageName);
                }
                context.importedPackages.putAll(info.importedPackages);
            }
            return context;
        }

        private StubSpec stubFor(String example) {
            ParsedMissingType parsed = ParsedMissingType.parse(example);
            if (parsed == null) {
                return null;
            }
            String packageName = importedPackages.get(parsed.simpleName);
            if (packageName == null) {
                packageName = firstFallbackPackage();
            }
            if (packageName == null || packageName.isEmpty()) {
                return null;
            }
            return new StubSpec(packageName, parsed.simpleName, parsed.kind);
        }

        private void addMemberDiagnostics(String runError, StubIndex index) {
            String normalized = runError.replace("\\n", "\n").replace("\\r", "\r");
            Matcher methodMatcher = MISSING_METHOD_PATTERN.matcher(normalized);
            while (methodMatcher.find()) {
                String methodName = methodMatcher.group(1);
                String parameters = methodMatcher.group(2);
                String ownerType = methodMatcher.group(3);
                StubSpec owner = index.findBySimpleName(ownerType);
                if (owner == null) {
                    owner = stubFor("class " + ownerType);
                    if (owner != null) {
                        index.put(owner);
                    }
                }
                if (owner != null && "class".equals(owner.kind)) {
                    owner.addMethod(methodName, parameters);
                }
            }
            Matcher nestedEnumMatcher = NESTED_ENUM_USAGE_PATTERN.matcher(normalized);
            while (nestedEnumMatcher.find()) {
                String ownerType = nestedEnumMatcher.group(1);
                String enumName = nestedEnumMatcher.group(2);
                String constant = nestedEnumMatcher.group(3);
                StubSpec owner = index.findBySimpleName(ownerType);
                if (owner == null) {
                    owner = stubFor("class " + ownerType);
                    if (owner != null) {
                        index.put(owner);
                    }
                }
                if (owner != null && "class".equals(owner.kind)) {
                    owner.addNestedEnumConstant(enumName, constant);
                }
            }
        }

        private String firstFallbackPackage() {
            return fallbackPackages.isEmpty() ? null : fallbackPackages.iterator().next();
        }

        private static List<String> diagnosticSources(String runError) {
            String normalized = runError.replace("\\n", "\n").replace("\\r", "\r");
            List<String> sources = new ArrayList<>();
            Matcher matcher = DIAGNOSTIC_SOURCE_PATTERN.matcher(normalized);
            while (matcher.find()) {
                String source = matcher.group(1);
                if (!sources.contains(source)) {
                    sources.add(source);
                }
            }
            return sources;
        }

        private static File resolveDiagnosticSource(String diagnosticSource, File projectRoot) {
            File direct = new File(diagnosticSource);
            if (direct.isFile()) {
                return direct;
            }
            if (projectRoot == null) {
                return null;
            }
            String normalized = diagnosticSource.replace('\\', '/');
            String suffix = sourceSuffix(normalized);
            if (suffix.isEmpty()) {
                return null;
            }
            File resolved = new File(projectRoot, suffix);
            return resolved.isFile() ? resolved : null;
        }

        private static String sourceSuffix(String sourcePath) {
            for (String marker : new String[] {"/src/main/java/", "/src/test/java/", "/java/"}) {
                int index = sourcePath.indexOf(marker);
                if (index >= 0) {
                    return sourcePath.substring(index + marker.length());
                }
            }
            return "";
        }

        private static String inferPackageFromPath(String sourcePath) {
            String suffix = sourceSuffix(sourcePath.replace('\\', '/'));
            if (suffix.isEmpty() || !suffix.endsWith(".java")) {
                return "";
            }
            int slash = suffix.lastIndexOf('/');
            if (slash < 0) {
                return "";
            }
            return suffix.substring(0, slash).replace('/', '.');
        }
    }

    private static final class SourceInfo {
        private final String packageName;
        private final Map<String, String> importedPackages;

        private SourceInfo(String packageName, Map<String, String> importedPackages) {
            this.packageName = packageName;
            this.importedPackages = importedPackages;
        }

        private static SourceInfo read(File sourceFile) {
            if (sourceFile == null || !sourceFile.isFile()) {
                return null;
            }
            String source;
            try {
                source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not read source file: " + sourceFile, e);
            }
            String packageName = "";
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
            if (packageMatcher.find()) {
                packageName = packageMatcher.group(1);
            }
            Map<String, String> imports = new LinkedHashMap<>();
            Matcher importMatcher = IMPORT_PATTERN.matcher(source);
            while (importMatcher.find()) {
                String imported = importMatcher.group(1);
                int dot = imported.lastIndexOf('.');
                if (dot < 0) {
                    continue;
                }
                imports.put(imported.substring(dot + 1), imported.substring(0, dot));
            }
            return new SourceInfo(packageName, imports);
        }
    }

    private static final class ParsedMissingType {
        private final String kind;
        private final String simpleName;

        private ParsedMissingType(String kind, String simpleName) {
            this.kind = kind;
            this.simpleName = simpleName;
        }

        private static ParsedMissingType parse(String example) {
            if (example == null) {
                return null;
            }
            String[] parts = example.trim().split("\\s+");
            if (parts.length != 2) {
                return null;
            }
            String kind = parts[0];
            String simpleName = parts[1];
            if (!("class".equals(kind) || "interface".equals(kind) || "enum".equals(kind))) {
                return null;
            }
            if (!simpleName.matches("[A-Z][A-Za-z0-9_$]*")) {
                return null;
            }
            return new ParsedMissingType(kind, simpleName);
        }
    }

    private static final class StubSpec {
        private final String packageName;
        private final String simpleName;
        private final String kind;
        private final Map<String, String> methods = new LinkedHashMap<>();
        private final Map<String, Set<String>> nestedEnums = new LinkedHashMap<>();

        private StubSpec(String packageName, String simpleName, String kind) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.kind = kind;
        }

        private String key() {
            return packageName + "." + simpleName;
        }

        private String source() {
            StringBuilder source = new StringBuilder();
            source.append("package ").append(packageName).append(";\n\n");
            if ("interface".equals(kind)) {
                source.append("public interface ").append(simpleName).append(" {}\n");
            } else if ("enum".equals(kind)) {
                source.append("public enum ").append(simpleName).append(" {}\n");
            } else {
                source.append("public class ").append(simpleName).append(" {\n");
                source.append("  public ").append(simpleName).append("() {}\n");
                source.append("  public ").append(simpleName).append("(Object... ignored) {}\n");
                for (Map.Entry<String, Set<String>> entry : nestedEnums.entrySet()) {
                    source.append("  public enum ")
                            .append(entry.getKey())
                            .append(" { ");
                    boolean first = true;
                    for (String constant : entry.getValue()) {
                        if (!first) {
                            source.append(", ");
                        }
                        source.append(constant);
                        first = false;
                    }
                    source.append(" }\n");
                }
                for (Map.Entry<String, String> method : methods.entrySet()) {
                    source.append("  ")
                            .append(methodDeclaration(method.getKey(), method.getValue()))
                            .append("\n");
                }
                source.append("}\n");
            }
            return source.toString();
        }

        private void addMethod(String methodName, String parameters) {
            methods.put(methodName, parameters);
        }

        private void addNestedEnumConstant(String enumName, String constant) {
            Set<String> constants = nestedEnums.get(enumName);
            if (constants == null) {
                constants = new LinkedHashSet<>();
                nestedEnums.put(enumName, constants);
            }
            constants.add(constant);
        }

        private String methodDeclaration(String methodName, String parameters) {
            String declarationParameters = parameterDeclarations(parameters);
            if ("getEntityType".equals(methodName) && nestedEnums.containsKey("EntityType")) {
                return "public EntityType "
                        + methodName
                        + "("
                        + declarationParameters
                        + ") { return EntityType.values()[0]; }";
            }
            if (methodName.startsWith("has") || methodName.startsWith("is")) {
                return "public boolean "
                        + methodName
                        + "("
                        + declarationParameters
                        + ") { return false; }";
            }
            if ("getInstances".equals(methodName)
                    || "getRegisteredApplications".equals(methodName)) {
                return "public <T> java.util.List<T> "
                        + methodName
                        + "("
                        + declarationParameters
                        + ") { return java.util.Collections.emptyList(); }";
            }
            return "public <T> T "
                    + methodName
                    + "("
                    + declarationParameters
                    + ") { return null; }";
        }

        private static String parameterDeclarations(String parameters) {
            String trimmed = parameters == null ? "" : parameters.trim();
            if (trimmed.isEmpty()) {
                return "";
            }
            String[] types = trimmed.split(",");
            List<String> declarations = new ArrayList<>();
            for (int index = 0; index < types.length; index++) {
                String type = types[index].trim();
                if (type.isEmpty()) {
                    continue;
                }
                declarations.add("Object arg" + index);
            }
            return String.join(", ", declarations);
        }
    }

    private static final class Arguments {
        private final List<File> inputFiles;
        private final File outputDirectory;
        private final File sourceListOutputFile;
        private final File projectRoot;

        private Arguments(
                List<File> inputFiles,
                File outputDirectory,
                File sourceListOutputFile,
                File projectRoot) {
            this.inputFiles = inputFiles;
            this.outputDirectory = outputDirectory;
            this.sourceListOutputFile = sourceListOutputFile;
            this.projectRoot = projectRoot;
        }

        private static Arguments parse(String[] args) {
            List<File> inputFiles = new ArrayList<>();
            File outputDirectory = new File("build/inference-localization-study/stubs");
            File sourceListOutputFile = null;
            File projectRoot = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--csv".equals(arg)) {
                    inputFiles.add(new File(requiredValue(args, ++i, arg)));
                } else if ("--out-dir".equals(arg)) {
                    outputDirectory = new File(requiredValue(args, ++i, arg));
                } else if ("--source-list-out".equals(arg)) {
                    sourceListOutputFile = new File(requiredValue(args, ++i, arg));
                } else if ("--project-root".equals(arg)) {
                    projectRoot = new File(requiredValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFiles.isEmpty()) {
                throw new IllegalArgumentException("At least one --csv is required.");
            }
            return new Arguments(inputFiles, outputDirectory, sourceListOutputFile, projectRoot);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
