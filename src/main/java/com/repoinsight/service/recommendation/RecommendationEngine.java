package com.repoinsight.service.recommendation;

import com.repoinsight.model.*;
import com.repoinsight.util.PathUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates prioritised {@link Recommendation}s from coverage mappings.
 */
@Service
public class RecommendationEngine {

    /**
     * Generates recommendations from coverage mappings.
     */
    public List<Recommendation> generate(List<CoverageMapping> mappings) {
        List<Recommendation> recommendations = new ArrayList<>();

        for (CoverageMapping mapping : mappings) {
            Feature feature = mapping.getFeature();
            if (feature == null) continue;

            switch (mapping.getCoverageStatus()) {
                case NOT_COVERED -> recommendations.addAll(buildNotCoveredRecommendations(mapping, feature));
                case PARTIALLY_COVERED -> recommendations.addAll(buildPartialRecommendations(mapping, feature));
                case COVERED -> recommendations.addAll(buildCoveredGapRecommendations(mapping, feature));
            }
        }

        // Sort by priority
        recommendations.sort((a, b) -> a.getPriority().compareTo(b.getPriority()));
        return recommendations;
    }

    // ── Not Covered ───────────────────────────────────────────────────────────

    private List<Recommendation> buildNotCoveredRecommendations(CoverageMapping mapping, Feature feature) {
        List<Recommendation> recs = new ArrayList<>();

        Recommendation r = new Recommendation();
        r.setId(PathUtil.newId());
        r.setTitle("Add tests for: " + feature.getName());
        r.setDescription(
                "Feature '" + feature.getName() + "' in module '" + feature.getModule()
                + "' has no matching test coverage. "
                + "Evidence: " + evidenceSummary(feature));
        r.setPriority(computePriority(feature));
        r.setSeverity(feature.getSeverity() != null ? feature.getSeverity() : Feature.Severity.MEDIUM);
        r.setCategory(Recommendation.Category.MISSING_COVERAGE);
        r.setBusinessImpact(businessImpact(feature));
        r.setRationale(mapping.getMappingRationale());
        r.setEvidenceLinks(feature.getEvidenceLinks());
        r.setSuggestedTestTypes(List.of("Happy Path", "Negative Path", "Boundary Test"));
        recs.add(r);

        return recs;
    }

    // ── Partially Covered ─────────────────────────────────────────────────────

    private List<Recommendation> buildPartialRecommendations(CoverageMapping mapping, Feature feature) {
        List<Recommendation> recs = new ArrayList<>();

        if (!mapping.isHasNegativePath()) {
            Recommendation r = new Recommendation();
            r.setId(PathUtil.newId());
            r.setTitle("Add negative/error path tests for: " + feature.getName());
            r.setDescription(
                    "Feature '" + feature.getName() + "' has partial coverage but lacks negative/error path tests.");
            r.setPriority(Recommendation.Priority.P1);
            r.setSeverity(Feature.Severity.HIGH);
            r.setCategory(Recommendation.Category.NEGATIVE_PATH_GAP);
            r.setBusinessImpact("Unhandled errors could cause failures in production.");
            r.setRationale("Partial match found but no negative scenarios detected.");
            r.setEvidenceLinks(feature.getEvidenceLinks());
            r.setSuggestedTestTypes(List.of("Negative Path", "Error Handling", "Invalid Input"));
            recs.add(r);
        }

        if (!mapping.isHasBoundaryTest()) {
            Recommendation r = new Recommendation();
            r.setId(PathUtil.newId());
            r.setTitle("Add boundary tests for: " + feature.getName());
            r.setDescription("Feature '" + feature.getName() + "' lacks boundary/edge case test coverage.");
            r.setPriority(Recommendation.Priority.P2);
            r.setSeverity(Feature.Severity.MEDIUM);
            r.setCategory(Recommendation.Category.BOUNDARY_GAP);
            r.setBusinessImpact("Boundary conditions are common sources of production defects.");
            r.setRationale("No boundary test scenarios detected for this feature.");
            r.setEvidenceLinks(feature.getEvidenceLinks());
            r.setSuggestedTestTypes(List.of("Boundary Test", "Edge Case"));
            recs.add(r);
        }

        if (!mapping.isHasSecurityTest()
                && feature.getInterfaceType() != null
                && (feature.getInterfaceType().contains("API") || feature.getInterfaceType().contains("UI"))) {
            Recommendation r = new Recommendation();
            r.setId(PathUtil.newId());
            r.setTitle("Add security/auth tests for: " + feature.getName());
            r.setDescription("API/UI feature '" + feature.getName() + "' lacks authentication/authorization tests.");
            r.setPriority(Recommendation.Priority.P1);
            r.setSeverity(Feature.Severity.CRITICAL);
            r.setCategory(Recommendation.Category.SECURITY_GAP);
            r.setBusinessImpact("Missing auth tests can lead to unauthorized access vulnerabilities.");
            r.setRationale("No security scenarios detected for this API/UI feature.");
            r.setEvidenceLinks(feature.getEvidenceLinks());
            r.setSuggestedTestTypes(List.of("Security", "Authorization", "Authentication"));
            recs.add(r);
        }

        return recs;
    }

    // ── Covered but with gaps ─────────────────────────────────────────────────

    private List<Recommendation> buildCoveredGapRecommendations(CoverageMapping mapping, Feature feature) {
        List<Recommendation> recs = new ArrayList<>();

        // Even covered features may be missing security tests
        if (!mapping.isHasSecurityTest()
                && feature.getSeverity() == Feature.Severity.CRITICAL
                && feature.getInterfaceType() != null
                && feature.getInterfaceType().contains("API")) {
            Recommendation r = new Recommendation();
            r.setId(PathUtil.newId());
            r.setTitle("Strengthen security testing for: " + feature.getName());
            r.setDescription("High-severity API feature '" + feature.getName() + "' is covered but lacks explicit security tests.");
            r.setPriority(Recommendation.Priority.P1);
            r.setSeverity(Feature.Severity.HIGH);
            r.setCategory(Recommendation.Category.SECURITY_GAP);
            r.setBusinessImpact("Security vulnerabilities in critical APIs can have serious business impact.");
            r.setRationale("Feature is marked covered but no security-tagged scenarios found.");
            r.setEvidenceLinks(feature.getEvidenceLinks());
            r.setSuggestedTestTypes(List.of("Security", "Authorization"));
            recs.add(r);
        }

        return recs;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Recommendation.Priority computePriority(Feature feature) {
        if (feature.getSeverity() == Feature.Severity.CRITICAL) return Recommendation.Priority.P0;
        if (feature.getSeverity() == Feature.Severity.HIGH)     return Recommendation.Priority.P1;
        if (feature.getSeverity() == Feature.Severity.MEDIUM)   return Recommendation.Priority.P2;
        return Recommendation.Priority.P3;
    }

    private String businessImpact(Feature feature) {
        return switch (feature.getSeverity() != null ? feature.getSeverity() : Feature.Severity.MEDIUM) {
            case CRITICAL -> "Critical: Untested functionality may cause complete feature failure or data loss.";
            case HIGH     -> "High: Missing tests increase risk of regressions in key user flows.";
            case MEDIUM   -> "Medium: Untested feature may cause intermittent issues affecting users.";
            case LOW      -> "Low: Minor functionality gap; low business impact.";
        };
    }

    private String evidenceSummary(Feature feature) {
        if (feature.getEvidenceLinks() == null || feature.getEvidenceLinks().isEmpty()) {
            return "no source evidence links available";
        }
        return feature.getEvidenceLinks().stream()
                .map(EvidenceLink::getDisplayLabel)
                .limit(3)
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");
    }
}
