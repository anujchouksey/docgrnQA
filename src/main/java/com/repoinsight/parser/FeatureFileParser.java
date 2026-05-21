package com.repoinsight.parser;

import com.repoinsight.model.EvidenceLink;
import com.repoinsight.model.TestAsset;
import com.repoinsight.util.PathUtil;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Gherkin {@code .feature} files to produce {@link TestAsset} instances.
 */
@Component
public class FeatureFileParser {

    private static final Pattern FEATURE_PATTERN     = Pattern.compile("^\\s*Feature:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern SCENARIO_PATTERN    = Pattern.compile("^\\s*(?:Scenario(?:\\s+Outline)?|Example):\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TAG_PATTERN         = Pattern.compile("@(\\S+)");
    private static final Pattern STEP_PATTERN        = Pattern.compile("^\\s*(Given|When|Then|And|But)\\s+(.+)$", Pattern.MULTILINE);

    /**
     * Parses a single .feature file and returns a TestAsset.
     */
    public TestAsset parse(Path file, Path repoRoot, String content) {
        TestAsset asset = new TestAsset();
        asset.setId(PathUtil.newId());
        asset.setFilePath(file.toString());
        asset.setRelativePath(PathUtil.normalise(repoRoot.relativize(file).toString()));
        asset.setTestType(TestAsset.TestType.FEATURE_FILE);

        // Feature title
        Matcher fm = FEATURE_PATTERN.matcher(content);
        if (fm.find()) {
            asset.setTitle(fm.group(1).trim());
        } else {
            asset.setTitle(file.getFileName().toString().replace(".feature", ""));
        }

        // Tags
        List<String> tags = new ArrayList<>();
        Matcher tm = TAG_PATTERN.matcher(content);
        while (tm.find()) {
            tags.add(tm.group(1));
        }
        asset.setTags(tags);

        // Scenario titles
        List<String> scenarios = new ArrayList<>();
        Matcher sm = SCENARIO_PATTERN.matcher(content);
        int lineNumber = 1;
        while (sm.find()) {
            scenarios.add(sm.group(1).trim());
        }
        asset.setScenarioTitles(scenarios);

        // Step keywords
        List<String> steps = new ArrayList<>();
        Matcher stm = STEP_PATTERN.matcher(content);
        while (stm.find()) {
            steps.add(stm.group(1) + " " + stm.group(2).trim());
        }
        asset.setStepKeywords(steps.size() > 20 ? steps.subList(0, 20) : steps);

        // Evidence
        EvidenceLink link = new EvidenceLink(file.toString(), asset.getRelativePath(), 1, EvidenceLink.EvidenceType.SCENARIO);
        asset.setEvidenceLinks(List.of(link));

        // Infer module from path
        asset.setModule(inferModule(asset.getRelativePath()));

        // Classify intent
        asset.setIntentType(classifyIntent(content, tags));

        return asset;
    }

    private TestAsset.IntentType classifyIntent(String content, List<String> tags) {
        String lower = content.toLowerCase();
        String tagStr = String.join(" ", tags).toLowerCase();
        if (lower.contains("invalid") || lower.contains("error") || lower.contains("fail")
                || lower.contains("negative") || tagStr.contains("negative")) {
            return TestAsset.IntentType.NEGATIVE;
        }
        if (lower.contains("boundary") || lower.contains("edge case") || lower.contains("limit")
                || tagStr.contains("boundary")) {
            return TestAsset.IntentType.BOUNDARY;
        }
        if (lower.contains("unauthori") || lower.contains("permission") || lower.contains("security")
                || lower.contains("auth") || tagStr.contains("security") || tagStr.contains("auth")) {
            return TestAsset.IntentType.SECURITY_AUTH;
        }
        if (lower.contains("integration") || tagStr.contains("integration")) {
            return TestAsset.IntentType.INTEGRATION;
        }
        return TestAsset.IntentType.HAPPY_PATH;
    }

    private String inferModule(String relativePath) {
        if (relativePath == null) return "general";
        String[] parts = relativePath.split("/");
        // Look for a meaningful directory name that is not top-level generic names
        for (int i = parts.length - 2; i >= 0; i--) {
            String part = parts[i];
            if (!part.equalsIgnoreCase("features") && !part.equalsIgnoreCase("test")
                    && !part.equalsIgnoreCase("tests") && !part.equalsIgnoreCase("bdd")
                    && !part.equalsIgnoreCase("e2e") && !part.equalsIgnoreCase("src")) {
                return part;
            }
        }
        return parts.length > 0 ? parts[0] : "general";
    }
}
