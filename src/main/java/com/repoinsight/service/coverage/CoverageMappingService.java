package com.repoinsight.service.coverage;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.*;
import com.repoinsight.util.TextSimilarityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps dev-repo {@link Feature}s to QA-repo {@link TestAsset}s and computes coverage.
 */
@Service
public class CoverageMappingService {

    private static final Logger log = LoggerFactory.getLogger(CoverageMappingService.class);

    private final AppProperties       properties;
    private final TextSimilarityUtil  similarity;

    public CoverageMappingService(AppProperties properties, TextSimilarityUtil similarity) {
        this.properties = properties;
        this.similarity = similarity;
    }

    /**
     * Produces a {@link CoverageMapping} for every feature against the full set of test assets.
     */
    public List<CoverageMapping> map(List<Feature> features, List<TestAsset> testAssets) {
        log.info("Mapping {} features against {} test assets", features.size(), testAssets.size());
        List<CoverageMapping> mappings = new ArrayList<>();

        for (Feature feature : features) {
            CoverageMapping mapping = computeMapping(feature, testAssets);
            mappings.add(mapping);
        }

        log.info("Mapping complete: {} mappings produced", mappings.size());
        return mappings;
    }

    // ── Mapping computation ───────────────────────────────────────────────────

    private CoverageMapping computeMapping(Feature feature, List<TestAsset> testAssets) {
        CoverageMapping mapping = new CoverageMapping();
        mapping.setFeatureId(feature.getId());
        mapping.setFeature(feature);

        AppProperties.Coverage.Scoring scoring = properties.getCoverage().getScoring();
        List<ScoredAsset> scored = new ArrayList<>();

        for (TestAsset asset : testAssets) {
            double score = computeScore(feature, asset, scoring);
            if (score > 0.1) {
                scored.add(new ScoredAsset(asset, score, inferMatchedBy(feature, asset, scoring)));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredAsset::score).reversed());

        // Collect matched assets above threshold
        List<TestAsset> matched = scored.stream()
                .filter(s -> s.score() >= 0.25)
                .map(ScoredAsset::asset)
                .collect(Collectors.toList());

        mapping.setMatchedTests(matched);
        mapping.setMappingScore(scored.isEmpty() ? 0.0 : scored.get(0).score());
        mapping.setMatchedBy(scored.isEmpty() ? List.of() : scored.get(0).matchedBy());

        // Classify coverage
        classify(mapping, scored, scoring);

        // Analyse test types present
        analyseCoverage(mapping, matched);

        return mapping;
    }

    private double computeScore(Feature feature, TestAsset asset, AppProperties.Coverage.Scoring scoring) {
        double score = 0.0;

        String featureName  = feature.getName();
        String featureDesc  = feature.getDescription();
        String featureModule = feature.getModule();

        // Exact match on normalized name
        if (checkExactMatch(featureName, asset)) {
            score = Math.max(score, scoring.getExactMatchWeight());
        }

        // Token overlap against all text in test asset
        String assetText = buildAssetText(asset);
        double tokenScore = similarity.tokenOverlap(featureName, assetText);
        score = Math.max(score, tokenScore * scoring.getTokenOverlapWeight());

        // Path / module alignment
        if (featureModule != null && asset.getModule() != null) {
            double modScore = similarity.combinedSimilarity(featureModule, asset.getModule());
            score = Math.max(score, modScore * scoring.getPathAlignmentWeight());
        }

        // Fuzzy similarity
        double fuzzyScore = similarity.fuzzyMatch(featureName, assetText);
        score = Math.max(score, fuzzyScore * scoring.getFuzzyWeight());

        // Tag matching
        if (!feature.getTags().isEmpty() && !asset.getTags().isEmpty()) {
            long commonTags = feature.getTags().stream()
                    .filter(t -> asset.getTags().stream().anyMatch(at -> at.equalsIgnoreCase(t)))
                    .count();
            if (commonTags > 0) {
                score = Math.max(score, scoring.getTagMatchWeight());
            }
        }

        // Description / scenario title overlap
        if (featureDesc != null && !asset.getScenarioTitles().isEmpty()) {
            for (String title : asset.getScenarioTitles()) {
                double ts = similarity.tokenOverlap(featureName, title);
                score = Math.max(score, ts * scoring.getTokenOverlapWeight());
            }
        }

        return Math.min(score, 1.0);
    }

    private boolean checkExactMatch(String featureName, TestAsset asset) {
        if (similarity.exactMatch(featureName, asset.getTitle())) return true;
        return asset.getScenarioTitles().stream()
                .anyMatch(t -> similarity.exactMatch(featureName, t));
    }

    private String buildAssetText(TestAsset asset) {
        StringBuilder sb = new StringBuilder();
        if (asset.getTitle() != null) sb.append(asset.getTitle()).append(" ");
        asset.getScenarioTitles().forEach(t -> sb.append(t).append(" "));
        asset.getTags().forEach(t -> sb.append(t).append(" "));
        asset.getStepKeywords().forEach(s -> sb.append(s).append(" "));
        return sb.toString();
    }

    private List<String> inferMatchedBy(Feature feature, TestAsset asset,
                                         AppProperties.Coverage.Scoring scoring) {
        List<String> methods = new ArrayList<>();
        if (checkExactMatch(feature.getName(), asset)) methods.add("exact");
        if (similarity.tokenOverlap(feature.getName(), buildAssetText(asset)) >= 0.3) methods.add("token");
        if (feature.getModule() != null && asset.getModule() != null
                && similarity.combinedSimilarity(feature.getModule(), asset.getModule()) >= 0.5)
            methods.add("module");
        if (similarity.fuzzyMatch(feature.getName(), asset.getTitle()) >= 0.7) methods.add("fuzzy");
        if (!feature.getTags().isEmpty() && !asset.getTags().isEmpty()
                && feature.getTags().stream().anyMatch(t -> asset.getTags().contains(t)))
            methods.add("tag");
        return methods.isEmpty() ? List.of("heuristic") : methods;
    }

    private void classify(CoverageMapping mapping, List<ScoredAsset> scored,
                           AppProperties.Coverage.Scoring scoring) {
        if (scored.isEmpty() || mapping.getMappingScore() < 0.1) {
            mapping.setCoverageStatus(CoverageMapping.CoverageStatus.NOT_COVERED);
            mapping.setMappingStrength(CoverageMapping.MappingStrength.NOT_MAPPED);
            mapping.setMappingRationale("No matching test assets found");
            return;
        }

        double topScore = mapping.getMappingScore();

        if (topScore >= scoring.getMinCoveredThreshold()) {
            mapping.setCoverageStatus(CoverageMapping.CoverageStatus.COVERED);
            mapping.setMappingStrength(topScore >= 0.9
                    ? CoverageMapping.MappingStrength.CONFIRMED
                    : CoverageMapping.MappingStrength.LIKELY);
            mapping.setMappingRationale("Strong match found (score: " + String.format("%.2f", topScore) + ")");
        } else if (topScore >= scoring.getMinPartialThreshold()) {
            mapping.setCoverageStatus(CoverageMapping.CoverageStatus.PARTIALLY_COVERED);
            mapping.setMappingStrength(topScore >= 0.5
                    ? CoverageMapping.MappingStrength.POSSIBLE
                    : CoverageMapping.MappingStrength.NOT_MAPPED);
            mapping.setMappingRationale("Partial match found (score: " + String.format("%.2f", topScore) + ")");
        } else {
            mapping.setCoverageStatus(CoverageMapping.CoverageStatus.UNCLEAR);
            mapping.setMappingStrength(CoverageMapping.MappingStrength.NOT_MAPPED);
            mapping.setMappingRationale("Weak match – uncertain coverage (score: " + String.format("%.2f", topScore) + ")");
        }
    }

    private void analyseCoverage(CoverageMapping mapping, List<TestAsset> matched) {
        for (TestAsset asset : matched) {
            switch (asset.getIntentType()) {
                case HAPPY_PATH      -> mapping.setHasHappyPath(true);
                case NEGATIVE        -> mapping.setHasNegativePath(true);
                case BOUNDARY        -> mapping.setHasBoundaryTest(true);
                case SECURITY_AUTH   -> mapping.setHasSecurityTest(true);
            }
        }
    }

    // ── Inner record ─────────────────────────────────────────────────────────

    private record ScoredAsset(TestAsset asset, double score, List<String> matchedBy) {}
}
