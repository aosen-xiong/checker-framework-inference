package checkers.inference.repair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Discovers Java source files for experiment batches. */
public final class InferenceRepairSourceDiscovery {
    private InferenceRepairSourceDiscovery() {}

    /** Source filtering strategy for pilot studies. */
    public enum SourceFilter {
        ALL,
        NULLNESS_RELEVANT
    }

    public static List<File> discoverJavaSources(List<File> sourceRoots) {
        return discoverJavaSources(sourceRoots, SourceFilter.ALL);
    }

    public static List<File> discoverJavaSources(
            List<File> sourceRoots, SourceFilter sourceFilter) {
        List<File> sourceFiles = new ArrayList<>();
        for (File sourceRoot : sourceRoots) {
            collectJavaSources(sourceRoot, sourceFilter, sourceFiles);
        }
        Collections.sort(sourceFiles);
        return sourceFiles;
    }

    public static List<File> readSourceList(File sourceListFile, File relativeRoot) {
        try {
            List<String> lines =
                    Files.readAllLines(sourceListFile.toPath(), StandardCharsets.UTF_8);
            List<File> files = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    File sourceFile = new File(trimmed);
                    if (!sourceFile.isAbsolute() && relativeRoot != null) {
                        sourceFile = new File(relativeRoot, trimmed);
                    }
                    files.add(sourceFile);
                }
            }
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Could not read source list: " + sourceListFile, e);
        }
    }

    private static void collectJavaSources(
            File file, SourceFilter sourceFilter, List<File> sourceFiles) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Source root does not exist: " + file);
        }
        if (file.isFile()) {
            if (file.getName().endsWith(".java") && accepts(file, sourceFilter)) {
                sourceFiles.add(file);
            }
            return;
        }
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            collectJavaSources(child, sourceFilter, sourceFiles);
        }
    }

    private static boolean accepts(File file, SourceFilter sourceFilter) {
        if (sourceFilter == SourceFilter.ALL) {
            return true;
        }
        String normalizedPath = file.getPath().replace(File.separatorChar, '/');
        if (normalizedPath.contains("/qual/")) {
            return false;
        }
        String source = read(file);
        if (source.contains("@interface")) {
            return false;
        }
        return source.contains("null")
                || source.contains("Nullable")
                || source.contains("NonNull")
                || source.contains("PolyNull")
                || source.contains("Optional")
                || source.contains("requireNonNull");
    }

    private static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read source file: " + file, e);
        }
    }
}
