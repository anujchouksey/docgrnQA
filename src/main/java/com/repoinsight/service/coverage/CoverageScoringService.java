package com.repoinsight.service.coverage;

import com.repoinsight.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes summary statistics and risk scores for a coverage analysis.
 */
@Service
public class CoverageScoringService {

    /**
     * Computes aggregate stats from a list of coverage mappings.
     */
    public AnalysisResult.CoverageSummaryStats computeSummary(List<CoverageMapping> mappings,
                                                               List<Recommendation> recommendations,
                                                               List<TestAsset> testAssets) {
        AnalysisResult.CoverageSummaryStats stats = new AnalysisResult.CoverageSummaryStats();

        int total = mappings.size();
        int covered = 0, partial = 0, notCovered = 0, unclear = 0;
        int criticalGaps = 0, highRiskUncovered = 0;

        for (CoverageMapping m : mappings) {
            switch (m.getCoverageStatus()) {
                case COVERED           -> covered++;
                case PARTIALLY_COVERED -> partial++;
                case NOT_COVERED       -> notCovered++;
                case UNCLEAR           -> unclear++;
            }

            Feature f = m.getFeature();
            if (m.getCoverageStatus() == CoverageMapping.CoverageStatus.NOT_COVERED) {
                if (f != null && f.getSeverity() == Feature.Severity.CRITICAL) criticalGaps++;
                if (f != null && f.getRisk() == Feature.Risk.HIGH) highRiskUncovered++;
            }
        }

        stats.setTotalFeatures(total);
        stats.setCoveredCount(covered);
        stats.setPartiallyCoveredCount(partial);
        stats.setNotCoveredCount(notCovered);
        stats.setUnclearCount(unclear);
        stats.setCriticalGapsCount(criticalGaps);
        stats.setHighRiskUncoveredCount(highRiskUncovered);
        stats.setTotalTestAssets(testAssets.size());

        double coveragePct = total == 0 ? 0.0
                : (covered + partial * 0.5) / total * 100.0;
        stats.setOverallCoveragePercent(Math.round(coveragePct * 100.0) / 100.0);

        long p0 = recommendations.stream()
                .filter(r -> r.getPriority() == Recommendation.Priority.P0).count();
        long p1 = recommendations.stream()
                .filter(r -> r.getPriority() == Recommendation.Priority.P1).count();
        stats.setP0RecommendationCount((int) p0);
        stats.setP1RecommendationCount((int) p1);
        stats.setTotalRecommendations(recommendations.size());

        // Estimate obsolete tests: test assets with no matching feature at reasonable score
        long obsolete = testAssets.stream()
                .filter(ta -> mappings.stream()
                        .noneMatch(m -> m.getMatchedTests().contains(ta) && m.getMappingScore() >= 0.3))
                .count();
        stats.setObsoleteTestsEstimate((int) obsolete);

        return stats;
    }
}
