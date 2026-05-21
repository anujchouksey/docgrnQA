package com.repoinsight.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The coverage mapping between a dev {@link Feature} and matching {@link TestAsset}s.
 */
public class CoverageMapping {

    public enum CoverageStatus { COVERED, PARTIALLY_COVERED, NOT_COVERED, UNCLEAR }

    public enum MappingStrength { CONFIRMED, LIKELY, POSSIBLE, NOT_MAPPED }

    private String featureId;
    private Feature feature;
    private List<TestAsset> matchedTests = new ArrayList<>();
    private CoverageStatus coverageStatus;
    private MappingStrength mappingStrength;
    private double mappingScore;         // 0.0 – 1.0
    private String mappingRationale;
    private List<String> matchedBy = new ArrayList<>();   // ["exact", "token", "fuzzy", "ai", ...]
    private boolean hasHappyPath;
    private boolean hasNegativePath;
    private boolean hasBoundaryTest;
    private boolean hasSecurityTest;
    private boolean aiEnriched;

    public CoverageMapping() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getFeatureId() { return featureId; }
    public void setFeatureId(String featureId) { this.featureId = featureId; }
    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }
    public List<TestAsset> getMatchedTests() { return matchedTests; }
    public void setMatchedTests(List<TestAsset> matchedTests) { this.matchedTests = matchedTests; }
    public CoverageStatus getCoverageStatus() { return coverageStatus; }
    public void setCoverageStatus(CoverageStatus coverageStatus) { this.coverageStatus = coverageStatus; }
    public MappingStrength getMappingStrength() { return mappingStrength; }
    public void setMappingStrength(MappingStrength mappingStrength) { this.mappingStrength = mappingStrength; }
    public double getMappingScore() { return mappingScore; }
    public void setMappingScore(double mappingScore) { this.mappingScore = mappingScore; }
    public String getMappingRationale() { return mappingRationale; }
    public void setMappingRationale(String mappingRationale) { this.mappingRationale = mappingRationale; }
    public List<String> getMatchedBy() { return matchedBy; }
    public void setMatchedBy(List<String> matchedBy) { this.matchedBy = matchedBy; }
    public boolean isHasHappyPath() { return hasHappyPath; }
    public void setHasHappyPath(boolean hasHappyPath) { this.hasHappyPath = hasHappyPath; }
    public boolean isHasNegativePath() { return hasNegativePath; }
    public void setHasNegativePath(boolean hasNegativePath) { this.hasNegativePath = hasNegativePath; }
    public boolean isHasBoundaryTest() { return hasBoundaryTest; }
    public void setHasBoundaryTest(boolean hasBoundaryTest) { this.hasBoundaryTest = hasBoundaryTest; }
    public boolean isHasSecurityTest() { return hasSecurityTest; }
    public void setHasSecurityTest(boolean hasSecurityTest) { this.hasSecurityTest = hasSecurityTest; }
    public boolean isAiEnriched() { return aiEnriched; }
    public void setAiEnriched(boolean aiEnriched) { this.aiEnriched = aiEnriched; }
}
