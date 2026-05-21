package com.repoinsight.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for scanning repository file trees.
 */
@Component
public class FileScanner {

    /**
     * Walks a directory tree and returns all file paths, respecting the given
     * include-extensions and exclude-dir rules.
     */
    public List<Path> scanFiles(Path root,
                                 List<String> includeExtensions,
                                 List<String> excludeDirs,
                                 int maxFileSizeKb,
                                 int maxFiles) throws IOException {

        List<Path> result = new ArrayList<>();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return result;
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                for (String excluded : excludeDirs) {
                    if (dirName.equals(excluded)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (result.size() >= maxFiles) {
                    return FileVisitResult.TERMINATE;
                }
                String name = file.getFileName().toString();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex < 0) return FileVisitResult.CONTINUE;
                String ext = name.substring(dotIndex);
                if (!includeExtensions.contains(ext.toLowerCase())) {
                    return FileVisitResult.CONTINUE;
                }
                long sizeKb = attrs.size() / 1024;
                if (sizeKb > maxFileSizeKb) {
                    return FileVisitResult.CONTINUE;
                }
                result.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE; // skip unreadable files
            }
        });

        return result;
    }

    /**
     * Returns a relative path string from root to file.
     */
    public String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return file.toString().replace('\\', '/');
        }
    }

    /**
     * Safely reads a file to string.  Returns empty string on error.
     */
    public String readSafely(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "";
        }
    }
}
