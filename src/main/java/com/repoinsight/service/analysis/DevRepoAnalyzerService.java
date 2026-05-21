package com.repoinsight.service.analysis;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.Feature;
import com.repoinsight.model.RepoSource;
import com.repoinsight.parser.JavaCodeParser;
import com.repoinsight.util.FileScanner;
import com.repoinsight.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes the development / application repository to extract candidate features.
 */
@Service
public class DevRepoAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(DevRepoAnalyzerService.class);

    private final AppProperties properties;
    private final FileScanner   fileScanner;
    private final JavaCodeParser javaCodeParser;

    public DevRepoAnalyzerService(AppProperties properties, FileScanner fileScanner, JavaCodeParser javaCodeParser) {
        this.properties     = properties;
        this.fileScanner    = fileScanner;
        this.javaCodeParser = javaCodeParser;
    }

    /**
     * Scans the dev repo and returns all inferred features.
     */
    public List<Feature> analyze(RepoSource source) {
        List<Feature> features = new ArrayList<>();
        if (!source.isValid()) {
            log.warn("Dev repo source is invalid: {}", source.getValidationError());
            return features;
        }

        Path root = Paths.get(source.getPath());
        AppProperties.Analysis cfg = properties.getAnalysis();

        try {
            List<Path> files = fileScanner.scanFiles(
                    root,
                    cfg.getIncludeExtensions(),
                    cfg.getExcludeDirs(),
                    cfg.getMaxFileSizeKb(),
                    cfg.getMaxFilesPerRepo());

            log.info("Dev repo analysis: scanning {} files in {}", files.size(), root);

            for (Path file : files) {
                String name = file.getFileName().toString();
                String ext  = PathUtil.extension(name);
                String content = fileScanner.readSafely(file);
                if (content.isBlank()) continue;

                List<Feature> parsed = switch (ext) {
                    case ".java" -> javaCodeParser.parse(file, root, content);
                    default      -> analyzeGenericFile(file, root, content);
                };
                features.addAll(parsed);
            }

            log.info("Dev repo analysis complete: {} features found", features.size());

        } catch (Exception e) {
            log.error("Error analyzing dev repo: {}", e.getMessage(), e);
        }

        return features;
    }

    /**
     * Generic analysis for non-Java files – looks for route definitions,
     * exported functions, or OpenAPI paths.
     */
    private List<Feature> analyzeGenericFile(Path file, Path root, String content) {
        List<Feature> features = new ArrayList<>();
        String relativePath = PathUtil.normalise(root.relativize(file).toString());
        String name = file.getFileName().toString();
        String ext  = PathUtil.extension(name);
        String lower = content.toLowerCase();

        // OpenAPI / Swagger YAML or JSON
        if ((ext.equals(".yaml") || ext.equals(".yml") || ext.equals(".json"))
                && (lower.contains("paths:") || lower.contains("\"paths\":")
                    || lower.contains("openapi") || lower.contains("swagger"))) {
            features.addAll(parseOpenApiPaths(content, relativePath, file));
        }

        // TypeScript / JavaScript Express-style routes
        if ((ext.equals(".ts") || ext.equals(".js"))
                && (lower.contains("router.") || lower.contains("app.get") || lower.contains("app.post"))) {
            features.addAll(parseJsRoutes(content, relativePath, file));
        }

        return features;
    }

    private List<Feature> parseOpenApiPaths(String content, String relativePath, Path file) {
        List<Feature> features = new ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:^\\s{2}|^\\s{4})((?:/[\\w/{}.]+)):", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher m = p.matcher(content);
        while (m.find()) {
            String path = m.group(1);
            Feature f = new Feature();
            f.setId(PathUtil.newId());
            f.setName("API Path: " + path);
            f.setDescription("OpenAPI path: " + path + " in " + relativePath);
            f.setModule("API");
            f.setInterfaceType("API");
            f.setConfidence(0.85);
            f.setSeverity(Feature.Severity.HIGH);
            f.setRisk(Feature.Risk.MEDIUM);
            com.repoinsight.model.EvidenceLink ev = new com.repoinsight.model.EvidenceLink(
                    file.toString(), relativePath, 1, com.repoinsight.model.EvidenceLink.EvidenceType.ENDPOINT);
            f.setEvidenceLinks(List.of(ev));
            features.add(f);
        }
        return features;
    }

    private List<Feature> parseJsRoutes(String content, String relativePath, Path file) {
        List<Feature> features = new ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:router|app)\\.(get|post|put|delete|patch)\\s*\\(\\s*['\"]([^'\"]+)['\"]",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(content);
        while (m.find()) {
            Feature f = new Feature();
            f.setId(PathUtil.newId());
            f.setName(m.group(1).toUpperCase() + " " + m.group(2));
            f.setDescription("JS route: " + m.group(1).toUpperCase() + " " + m.group(2));
            f.setModule("API");
            f.setInterfaceType("API");
            f.setConfidence(0.8);
            f.setSeverity(Feature.Severity.MEDIUM);
            f.setRisk(Feature.Risk.MEDIUM);
            com.repoinsight.model.EvidenceLink ev = new com.repoinsight.model.EvidenceLink(
                    file.toString(), relativePath, 1, com.repoinsight.model.EvidenceLink.EvidenceType.ENDPOINT);
            f.setEvidenceLinks(List.of(ev));
            features.add(f);
        }
        return features;
    }
}
