package com.repoinsight.service.analysis;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.EvidenceLink;
import com.repoinsight.model.RepoSource;
import com.repoinsight.model.TestAsset;
import com.repoinsight.parser.FeatureFileParser;
import com.repoinsight.util.FileScanner;
import com.repoinsight.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes the QA / BDD repository to extract test assets.
 */
@Service
public class QaRepoAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(QaRepoAnalyzerService.class);

    private static final Pattern STEP_DEF_JAVA = Pattern.compile(
            "@(Given|When|Then|And|But)\\(\"([^\"]+)\"\\)",
            Pattern.MULTILINE);
    private static final Pattern CUCUMBER_RUNNER = Pattern.compile(
            "@CucumberOptions|@RunWith\\s*\\(\\s*Cucumber",
            Pattern.CASE_INSENSITIVE);

    private final AppProperties      properties;
    private final FileScanner        fileScanner;
    private final FeatureFileParser  featureFileParser;

    public QaRepoAnalyzerService(AppProperties properties, FileScanner fileScanner, FeatureFileParser featureFileParser) {
        this.properties       = properties;
        this.fileScanner      = fileScanner;
        this.featureFileParser = featureFileParser;
    }

    /**
     * Scans the QA repo and returns all extracted test assets.
     */
    public List<TestAsset> analyze(RepoSource source) {
        List<TestAsset> assets = new ArrayList<>();
        if (!source.isValid()) {
            log.warn("QA repo source is invalid: {}", source.getValidationError());
            return assets;
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

            log.info("QA repo analysis: scanning {} files in {}", files.size(), root);

            for (Path file : files) {
                String ext     = PathUtil.extension(file.getFileName().toString());
                String content = fileScanner.readSafely(file);
                if (content.isBlank()) continue;

                switch (ext) {
                    case ".feature" -> assets.add(featureFileParser.parse(file, root, content));
                    case ".java"    -> assets.addAll(parseStepDefinitions(file, root, content));
                    case ".js", ".ts" -> assets.addAll(parseJsTestFile(file, root, content));
                    case ".py"      -> assets.addAll(parsePythonTestFile(file, root, content));
                }
            }

            log.info("QA repo analysis complete: {} test assets found", assets.size());

        } catch (Exception e) {
            log.error("Error analyzing QA repo: {}", e.getMessage(), e);
        }

        return assets;
    }

    // ── Step definitions ──────────────────────────────────────────────────────

    private List<TestAsset> parseStepDefinitions(Path file, Path root, String content) {
        List<TestAsset> assets = new ArrayList<>();
        if (!STEP_DEF_JAVA.matcher(content).find() && !CUCUMBER_RUNNER.matcher(content).find()) {
            return assets;
        }

        TestAsset asset = new TestAsset();
        asset.setId(PathUtil.newId());
        asset.setFilePath(file.toString());
        asset.setRelativePath(PathUtil.normalise(root.relativize(file).toString()));
        asset.setTestType(TestAsset.TestType.STEP_DEFINITION);
        asset.setTitle(file.getFileName().toString().replace(".java", ""));

        List<String> steps = new ArrayList<>();
        Matcher m = STEP_DEF_JAVA.matcher(content);
        while (m.find()) {
            steps.add(m.group(1) + " " + m.group(2));
        }
        asset.setStepKeywords(steps);

        EvidenceLink ev = new EvidenceLink(file.toString(), asset.getRelativePath(), 1, EvidenceLink.EvidenceType.METHOD);
        asset.setEvidenceLinks(List.of(ev));
        asset.setModule(inferModule(asset.getRelativePath()));

        assets.add(asset);
        return assets;
    }

    private List<TestAsset> parseJsTestFile(Path file, Path root, String content) {
        List<TestAsset> assets = new ArrayList<>();
        Pattern describeP = Pattern.compile("(?:describe|it|test)\\s*\\(['\"]([^'\"]+)['\"]");
        Matcher m = describeP.matcher(content);
        if (!m.find()) return assets;

        TestAsset asset = new TestAsset();
        asset.setId(PathUtil.newId());
        asset.setFilePath(file.toString());
        asset.setRelativePath(PathUtil.normalise(root.relativize(file).toString()));
        asset.setTestType(TestAsset.TestType.UNIT_TEST);
        asset.setTitle(file.getFileName().toString());

        List<String> titles = new ArrayList<>();
        m.reset();
        while (m.find()) titles.add(m.group(1));
        asset.setScenarioTitles(titles);

        EvidenceLink ev = new EvidenceLink(file.toString(), asset.getRelativePath(), 1, EvidenceLink.EvidenceType.FILE);
        asset.setEvidenceLinks(List.of(ev));
        asset.setModule(inferModule(asset.getRelativePath()));

        assets.add(asset);
        return assets;
    }

    private List<TestAsset> parsePythonTestFile(Path file, Path root, String content) {
        List<TestAsset> assets = new ArrayList<>();
        if (!file.getFileName().toString().startsWith("test_")
                && !file.getFileName().toString().endsWith("_test.py")) {
            return assets;
        }

        TestAsset asset = new TestAsset();
        asset.setId(PathUtil.newId());
        asset.setFilePath(file.toString());
        asset.setRelativePath(PathUtil.normalise(root.relativize(file).toString()));
        asset.setTestType(TestAsset.TestType.UNIT_TEST);
        asset.setTitle(file.getFileName().toString());

        Pattern defP = Pattern.compile("def\\s+(test_\\w+)\\s*\\(");
        List<String> methods = new ArrayList<>();
        Matcher m = defP.matcher(content);
        while (m.find()) methods.add(m.group(1));
        asset.setScenarioTitles(methods);

        EvidenceLink ev = new EvidenceLink(file.toString(), asset.getRelativePath(), 1, EvidenceLink.EvidenceType.FILE);
        asset.setEvidenceLinks(List.of(ev));
        asset.setModule(inferModule(asset.getRelativePath()));

        assets.add(asset);
        return assets;
    }

    private String inferModule(String relativePath) {
        if (relativePath == null) return "general";
        String[] parts = relativePath.split("/");
        for (int i = parts.length - 2; i >= 0; i--) {
            String p = parts[i];
            if (!p.equalsIgnoreCase("test") && !p.equalsIgnoreCase("tests")
                    && !p.equalsIgnoreCase("features") && !p.equalsIgnoreCase("steps")
                    && !p.equalsIgnoreCase("src") && !p.equalsIgnoreCase("java")) {
                return p;
            }
        }
        return "general";
    }
}
