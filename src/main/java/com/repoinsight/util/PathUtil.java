package com.repoinsight.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Path / URL utility helpers.
 */
public final class PathUtil {

    private PathUtil() {}

    /**
     * Infers a project name from a local path or remote URL.
     */
    public static String inferProjectName(String input) {
        if (input == null || input.isBlank()) return "unknown";
        String s = input.trim().replaceAll("\\.git$", "");
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        return slash >= 0 ? s.substring(slash + 1) : s;
    }

    /**
     * Tries to build a GitHub blob URL for a file.
     * remoteUrl may be like https://github.com/owner/repo or
     * https://github.com/owner/repo.git
     */
    public static String buildGitHubBlobUrl(String remoteUrl, String branch, String relativePath, int lineNumber) {
        if (remoteUrl == null || !remoteUrl.contains("github.com")) return null;
        String base = remoteUrl.replaceAll("\\.git$", "");
        String ref  = branch != null && !branch.isBlank() ? branch : "main";
        String url  = base + "/blob/" + ref + "/" + relativePath;
        if (lineNumber > 0) url += "#L" + lineNumber;
        return url;
    }

    /**
     * Returns a short unique ID suitable for entity IDs.
     */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Normalises a file path to use forward slashes.
     */
    public static String normalise(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    /**
     * Returns the extension of a file name (including the dot), or empty string.
     */
    public static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }

    /**
     * Returns true when the given URL looks like a Git remote URL.
     */
    public static boolean isGitUrl(String input) {
        if (input == null) return false;
        String s = input.trim().toLowerCase();
        return s.startsWith("http://") || s.startsWith("https://")
                || s.startsWith("git@") || s.startsWith("ssh://")
                || s.endsWith(".git");
    }
}
