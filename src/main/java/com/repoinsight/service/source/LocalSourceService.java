package com.repoinsight.service.source;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.RepoSource;
import com.repoinsight.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.stream.Stream;

/**
 * Handles local folder sources.
 */
@Service
public class LocalSourceService {

    private static final Logger log = LoggerFactory.getLogger(LocalSourceService.class);

    private final AppProperties properties;

    public LocalSourceService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates and enriches a local source.
     *
     * @param localPath path as entered by the user
     * @return a populated {@link RepoSource}
     */
    public RepoSource resolve(String localPath) {
        RepoSource source = new RepoSource(RepoSource.SourceType.LOCAL, localPath);
        source.setId(PathUtil.newId());

        if (localPath == null || localPath.isBlank()) {
            source.setValid(false);
            source.setValidationError("Local path must not be empty");
            return source;
        }

        // Normalize the path to remove any ".." traversal segments before touching the file system
        Path path;
        try {
            path = Paths.get(localPath.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            source.setValid(false);
            source.setValidationError("Invalid path characters: " + e.getMessage());
            return source;
        }

        if (!Files.exists(path)) {
            source.setValid(false);
            source.setValidationError("Path does not exist: " + localPath);
            return source;
        }
        if (!Files.isDirectory(path)) {
            source.setValid(false);
            source.setValidationError("Path is not a directory: " + localPath);
            return source;
        }

        source.setPath(path.toString());
        source.setResolvedName(PathUtil.inferProjectName(localPath));

        // Count files and size
        try (Stream<Path> files = Files.walk(path)) {
            long[] counts = {0, 0};
            files.filter(Files::isRegularFile).forEach(f -> {
                counts[0]++;
                try { counts[1] += Files.size(f); } catch (Exception ignored) {}
            });
            source.setTotalFiles((int) counts[0]);
            source.setFileSizeBytes(counts[1]);
        } catch (Exception e) {
            log.warn("Could not count files in {}: {}", localPath, e.getMessage());
        }

        // Detect dominant language
        source.setLanguage(detectLanguage(path));
        source.setValid(true);
        log.info("Resolved local source: {} ({} files)", source.getResolvedName(), source.getTotalFiles());
        return source;
    }

    private String detectLanguage(Path root) {
        int java = 0, ts = 0, js = 0, py = 0, rb = 0, go = 0, cs = 0;
        try (Stream<Path> files = Files.walk(root, 4)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                String ext = PathUtil.extension(f.getFileName().toString());
                switch (ext) {
                    case ".java" -> java++;
                    case ".ts"   -> ts++;
                    case ".js"   -> js++;
                    case ".py"   -> py++;
                    case ".rb"   -> rb++;
                    case ".go"   -> go++;
                    case ".cs"   -> cs++;
                }
            }
        } catch (Exception ignored) {}
        int max = Math.max(java, Math.max(ts, Math.max(js, Math.max(py, Math.max(rb, Math.max(go, cs))))));
        if (max == 0) return "Unknown";
        if (max == java) return "Java";
        if (max == ts)   return "TypeScript";
        if (max == js)   return "JavaScript";
        if (max == py)   return "Python";
        if (max == rb)   return "Ruby";
        if (max == go)   return "Go";
        return "C#";
    }
}
