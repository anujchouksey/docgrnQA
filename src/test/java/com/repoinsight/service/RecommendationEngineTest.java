package com.repoinsight.service;

import com.repoinsight.model.*;
import com.repoinsight.service.recommendation.RecommendationEngine;
import com.repoinsight.util.PathUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
    }

    private CoverageMapping mapping(CoverageMapping.CoverageStatus status,
                                    Feature.Severity severity,
                                    String interfaceType) {
        Feature f = new Feature();
        f.setId(PathUtil.newId());
        f.setName("Test Feature");
        f.setModule("TestModule");
        f.setSeverity(severity);
        f.setRisk(Feature.Risk.HIGH);
        f.setInterfaceType(interfaceType);

        CoverageMapping m = new CoverageMapping();
        m.setFeatureId(f.getId());
        m.setFeature(f);
        m.setCoverageStatus(status);
        m.setMappingStrength(CoverageMapping.MappingStrength.NOT_MAPPED);
        m.setMappingScore(0.0);
        m.setMappingRationale("Test rationale");
        return m;
    }

    @Test
    void generate_notCoveredCritical_producesP0Recommendation() {
        CoverageMapping m = mapping(CoverageMapping.CoverageStatus.NOT_COVERED,
                Feature.Severity.CRITICAL, "API");

        List<Recommendation> recs = engine.generate(List.of(m));

        assertThat(recs).isNotEmpty();
        assertThat(recs.get(0).getPriority()).isEqualTo(Recommendation.Priority.P0);
    }

    @Test
    void generate_notCoveredHigh_producesP1Recommendation() {
        CoverageMapping m = mapping(CoverageMapping.CoverageStatus.NOT_COVERED,
                Feature.Severity.HIGH, "SERVICE");

        List<Recommendation> recs = engine.generate(List.of(m));

        assertThat(recs).isNotEmpty();
        assertThat(recs.get(0).getPriority()).isEqualTo(Recommendation.Priority.P1);
    }

    @Test
    void generate_partiallyCovered_missingNegativePath_generatesRecommendation() {
        CoverageMapping m = mapping(CoverageMapping.CoverageStatus.PARTIALLY_COVERED,
                Feature.Severity.HIGH, "SERVICE");
        m.setHasNegativePath(false);
        m.setHasBoundaryTest(true);
        m.setHasSecurityTest(true);

        List<Recommendation> recs = engine.generate(List.of(m));

        assertThat(recs).anyMatch(r ->
                r.getCategory() == Recommendation.Category.NEGATIVE_PATH_GAP);
    }

    @Test
    void generate_partiallyCovered_missingBoundaryTest_generatesRecommendation() {
        CoverageMapping m = mapping(CoverageMapping.CoverageStatus.PARTIALLY_COVERED,
                Feature.Severity.MEDIUM, "SERVICE");
        m.setHasNegativePath(true);
        m.setHasBoundaryTest(false);
        m.setHasSecurityTest(true);

        List<Recommendation> recs = engine.generate(List.of(m));

        assertThat(recs).anyMatch(r ->
                r.getCategory() == Recommendation.Category.BOUNDARY_GAP);
    }

    @Test
    void generate_partiallyCoveredApi_missingSecurityTest_generatesSecurityRecommendation() {
        CoverageMapping m = mapping(CoverageMapping.CoverageStatus.PARTIALLY_COVERED,
                Feature.Severity.HIGH, "API");
        m.setHasNegativePath(true);
        m.setHasBoundaryTest(true);
        m.setHasSecurityTest(false);

        List<Recommendation> recs = engine.generate(List.of(m));

        assertThat(recs).anyMatch(r ->
                r.getCategory() == Recommendation.Category.SECURITY_GAP);
    }

    @Test
    void generate_emptyMappings_returnsEmptyList() {
        List<Recommendation> recs = engine.generate(List.of());
        assertThat(recs).isEmpty();
    }

    @Test
    void generate_sortedByPriority() {
        List<CoverageMapping> mappings = List.of(
                mapping(CoverageMapping.CoverageStatus.NOT_COVERED, Feature.Severity.LOW, "SERVICE"),
                mapping(CoverageMapping.CoverageStatus.NOT_COVERED, Feature.Severity.CRITICAL, "API"),
                mapping(CoverageMapping.CoverageStatus.NOT_COVERED, Feature.Severity.HIGH, "SERVICE")
        );

        List<Recommendation> recs = engine.generate(mappings);

        // First recommendation should have higher or equal priority to last
        if (recs.size() >= 2) {
            assertThat(recs.get(0).getPriority().compareTo(
                    recs.get(recs.size() - 1).getPriority())).isLessThanOrEqualTo(0);
        }
    }
}
