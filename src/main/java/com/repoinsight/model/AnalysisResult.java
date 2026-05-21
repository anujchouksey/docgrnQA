package com.repoinsight.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete result of a coverage or understanding analysis run.
 */
public class AnalysisResult {

    public enum AnalysisMode { COVERAGE, UNDERSTANDING }
    public enum AnalysisStatus { IN_PROGRESS, COMPLETED, FAILED }

    private String id;
    private AnalysisMode mode;
    private AnalysisStatus status;
    private String statusMessage;
    private Instant startedAt;
    private Instant completedAt;
    private boolean aiEnabled;

    // Coverage mode
    private RepoSource devRepo;
    private RepoSource qaRepo;
    private List<Feature> features = new ArrayList<>();
    private List<TestAsset> testAssets = new ArrayList<>();
    private List<CoverageMapping> coverageMappings = new ArrayList<>();
    private List<Recommendation> recommendations = new ArrayList<>();
    private CoverageSummaryStats coverageSummary;

    // Understanding mode
    private RepoSource targetRepo;
    private RepositoryInsight repositoryInsight;

    public AnalysisResult() {}

    // ── Nested summary stats ─────────────────────────────────────────────────

    public static class CoverageSummaryStats {
        private int totalFeatures;
        private int coveredCount;
        private int partiallyCoveredCount;
        private int notCoveredCount;
        private int unclearCount;
        private double overallCoveragePercent;
        private int criticalGapsCount;
        private int highRiskUncoveredCount;
        private int p0RecommendationCount;
        private int p1RecommendationCount;
        private int totalRecommendations;
        private int totalTestAssets;
        private int obsoleteTestsEstimate;

        public int getTotalFeatures() { return totalFeatures; }
        public void setTotalFeatures(int totalFeatures) { this.totalFeatures = totalFeatures; }
        public int getCoveredCount() { return coveredCount; }
        public void setCoveredCount(int coveredCount) { this.coveredCount = coveredCount; }
        public int getPartiallyCoveredCount() { return partiallyCoveredCount; }
        public void setPartiallyCoveredCount(int partiallyCoveredCount) { this.partiallyCoveredCount = partiallyCoveredCount; }
        public int getNotCoveredCount() { return notCoveredCount; }
        public void setNotCoveredCount(int notCoveredCount) { this.notCoveredCount = notCoveredCount; }
        public int getUnclearCount() { return unclearCount; }
        public void setUnclearCount(int unclearCount) { this.unclearCount = unclearCount; }
        public double getOverallCoveragePercent() { return overallCoveragePercent; }
        public void setOverallCoveragePercent(double overallCoveragePercent) { this.overallCoveragePercent = overallCoveragePercent; }
        public int getCriticalGapsCount() { return criticalGapsCount; }
        public void setCriticalGapsCount(int criticalGapsCount) { this.criticalGapsCount = criticalGapsCount; }
        public int getHighRiskUncoveredCount() { return highRiskUncoveredCount; }
        public void setHighRiskUncoveredCount(int highRiskUncoveredCount) { this.highRiskUncoveredCount = highRiskUncoveredCount; }
        public int getP0RecommendationCount() { return p0RecommendationCount; }
        public void setP0RecommendationCount(int p0RecommendationCount) { this.p0RecommendationCount = p0RecommendationCount; }
        public int getP1RecommendationCount() { return p1RecommendationCount; }
        public void setP1RecommendationCount(int p1RecommendationCount) { this.p1RecommendationCount = p1RecommendationCount; }
        public int getTotalRecommendations() { return totalRecommendations; }
        public void setTotalRecommendations(int totalRecommendations) { this.totalRecommendations = totalRecommendations; }
        public int getTotalTestAssets() { return totalTestAssets; }
        public void setTotalTestAssets(int totalTestAssets) { this.totalTestAssets = totalTestAssets; }
        public int getObsoleteTestsEstimate() { return obsoleteTestsEstimate; }
        public void setObsoleteTestsEstimate(int obsoleteTestsEstimate) { this.obsoleteTestsEstimate = obsoleteTestsEstimate; }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public AnalysisMode getMode() { return mode; }
    public void setMode(AnalysisMode mode) { this.mode = mode; }
    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus status) { this.status = status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public RepoSource getDevRepo() { return devRepo; }
    public void setDevRepo(RepoSource devRepo) { this.devRepo = devRepo; }
    public RepoSource getQaRepo() { return qaRepo; }
    public void setQaRepo(RepoSource qaRepo) { this.qaRepo = qaRepo; }
    public List<Feature> getFeatures() { return features; }
    public void setFeatures(List<Feature> features) { this.features = features; }
    public List<TestAsset> getTestAssets() { return testAssets; }
    public void setTestAssets(List<TestAsset> testAssets) { this.testAssets = testAssets; }
    public List<CoverageMapping> getCoverageMappings() { return coverageMappings; }
    public void setCoverageMappings(List<CoverageMapping> coverageMappings) { this.coverageMappings = coverageMappings; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }
    public CoverageSummaryStats getCoverageSummary() { return coverageSummary; }
    public void setCoverageSummary(CoverageSummaryStats coverageSummary) { this.coverageSummary = coverageSummary; }
    public RepoSource getTargetRepo() { return targetRepo; }
    public void setTargetRepo(RepoSource targetRepo) { this.targetRepo = targetRepo; }
    public RepositoryInsight getRepositoryInsight() { return repositoryInsight; }
    public void setRepositoryInsight(RepositoryInsight repositoryInsight) { this.repositoryInsight = repositoryInsight; }
}
