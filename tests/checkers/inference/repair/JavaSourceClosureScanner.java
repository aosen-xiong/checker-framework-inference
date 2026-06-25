package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds likely same-project Java source dependencies from package/import/type-reference syntax. */
public final class JavaSourceClosureScanner {
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?([A-Za-z_$][A-Za-z0-9_$.*]*)\\s*;");
    private static final Pattern TYPE_REFERENCE_PATTERN =
            Pattern.compile("\\b([A-Z][A-Za-z0-9_$]*)\\b");

    private static final Set<String> SAME_PACKAGE_EXCLUDED_TYPES = new LinkedHashSet<>();

    static {
        SAME_PACKAGE_EXCLUDED_TYPES.add("Boolean");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Byte");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Character");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Class");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Double");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Enum");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Exception");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Float");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Integer");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Iterable");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Long");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Math");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Object");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Override");
        SAME_PACKAGE_EXCLUDED_TYPES.add("RuntimeException");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Short");
        SAME_PACKAGE_EXCLUDED_TYPES.add("String");
        SAME_PACKAGE_EXCLUDED_TYPES.add("StringBuilder");
        SAME_PACKAGE_EXCLUDED_TYPES.add("SuppressWarnings");
        SAME_PACKAGE_EXCLUDED_TYPES.add("System");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Thread");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Throwable");
        SAME_PACKAGE_EXCLUDED_TYPES.add("Void");
    }

    public List<String> likelyDependencies(File sourceFile, String relativeSourcePath) {
        String source;
        try {
            source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + sourceFile, e);
        }
        String packageName = packageName(source);
        String sourceRoot = sourceRoot(relativeSourcePath, packageName);
        List<String> dependencies = new ArrayList<>();
        dependencies.addAll(importedDependencies(source, sourceRoot));
        dependencies.addAll(samePackageDependencies(source, relativeSourcePath, sourceRoot, packageName));
        return dependencies;
    }

    private static String packageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String sourceRoot(String relativeSourcePath, String packageName) {
        if (packageName.isEmpty()) {
            int slash = relativeSourcePath.lastIndexOf('/');
            return slash < 0 ? "" : relativeSourcePath.substring(0, slash + 1);
        }
        String packagePath = packageName.replace('.', '/') + "/";
        int index = relativeSourcePath.lastIndexOf(packagePath);
        return index < 0 ? "" : relativeSourcePath.substring(0, index);
    }

    private static List<String> importedDependencies(String source, String sourceRoot) {
        List<String> dependencies = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            boolean staticImport = matcher.group(1) != null;
            String imported = matcher.group(2);
            if (staticImport || imported.endsWith(".*") || isPlatformImport(imported)) {
                continue;
            }
            dependencies.add(sourceRoot + imported.replace('.', '/') + ".java");
        }
        return dependencies;
    }

    private static boolean isPlatformImport(String imported) {
        return imported.startsWith("java.")
                || imported.startsWith("javax.")
                || imported.startsWith("jdk.")
                || imported.startsWith("sun.");
    }

    private static List<String> samePackageDependencies(
            String source, String relativeSourcePath, String sourceRoot, String packageName) {
        List<String> dependencies = new ArrayList<>();
        if (packageName.isEmpty()) {
            return dependencies;
        }
        String packagePath = packageName.replace('.', '/') + "/";
        String currentType = simpleName(relativeSourcePath);
        Matcher matcher = TYPE_REFERENCE_PATTERN.matcher(stripCommentsAndStrings(source));
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String type = matcher.group(1);
            if (type.equals(currentType) || SAME_PACKAGE_EXCLUDED_TYPES.contains(type) || !seen.add(type)) {
                continue;
            }
            dependencies.add(sourceRoot + packagePath + type + ".java");
        }
        return dependencies;
    }

    private static String simpleName(String relativeSourcePath) {
        int slash = relativeSourcePath.lastIndexOf('/');
        String name = slash < 0 ? relativeSourcePath : relativeSourcePath.substring(slash + 1);
        return name.endsWith(".java") ? name.substring(0, name.length() - ".java".length()) : name;
    }

    private static String stripCommentsAndStrings(String source) {
        String noBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        String noLineComments = noBlockComments.replaceAll("(?m)//.*$", " ");
        return noLineComments.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
    }
}
