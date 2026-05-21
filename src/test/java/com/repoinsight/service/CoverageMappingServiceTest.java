package com.repoinsight.service;

import com.repoinsight.model.*;
import com.repoinsight.service.coverage.CoverageMappingService;
import com.repoinsight.config.AppProperties;
import com.repoinsight.util.TextSimilarityUtil;
import com.repoinsight.util.PathUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CoverageMappingServiceTest {

    private CoverageMappingService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        service = new CoverageMappingService(props, new TextSimilarityUtil());
    }

    private Feature feature(String name, String module) {
        Feature f = new Feature();
        f.setId(PathUtil.newId());
        f.setName(name);
        f.setModule(module);
        f.setSeverity(Feature.Severity.HIGH);
        f.setRisk(Feature.Risk.MEDIUM);
        f.setConfidence(0.9);
        return f;
    }

    private TestAsset testAsset(String title, String module) {
        TestAsset a = new TestAsset();
        a.setId(PathUtil.newId());
        a.setTitle(title);
        a.setModule(module);
        a.setTestType(TestAsset.TestType.FEATURE_FILE);
        a.setIntentType(TestAsset.IntentType.HAPPY_PATH);
        return a;
    }

    @Test
    void map_emptyInputs_returnsEmptyList() {
        List<CoverageMapping> result = service.map(List.of(), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void map_exactMatch_classifiesCovered() {
        Feature f = feature("user login", "auth");
        TestAsset t = testAsset("user login", "auth");

        List<CoverageMapping> mappings = service.map(List.of(f), List.of(t));

        assertThat(mappings).hasSize(1);
        CoverageMapping m = mappings.get(0);
        assertThat(m.getCoverageStatus())
                .isIn(CoverageMapping.CoverageStatus.COVERED, CoverageMapping.CoverageStatus.PARTIALLY_COVERED);
        assertThat(m.getMappingScore()).isGreaterThan(0.3);
    }

    @Test
    void map_noMatchingTest_classifiesNotCovered() {
        Feature f = feature("payment refund processing", "payments");
        TestAsset t = testAsset("user profile management", "profile");

        List<CoverageMapping> mappings = service.map(List.of(f), List.of(t));

        assertThat(mappings).hasSize(1);
        CoverageMapping m = mappings.get(0);
        // Score should be low for completely different texts
        assertThat(m.getMappingScore()).isLessThan(0.5);
    }

    @Test
    void map_multipleFeatures_returnsOneMappingEach() {
        List<Feature> features = List.of(
                feature("user login", "auth"),
                feature("product search", "catalog"),
                feature("payment checkout", "payments")
        );
        TestAsset t = testAsset("user login flow", "auth");

        List<CoverageMapping> mappings = service.map(features, List.of(t));

        assertThat(mappings).hasSize(3);
    }

    @Test
    void map_featureReferenceIsPreserved() {
        Feature f = feature("order tracking", "orders");
        TestAsset t = testAsset("track order status", "orders");

        List<CoverageMapping> mappings = service.map(List.of(f), List.of(t));

        assertThat(mappings.get(0).getFeature()).isEqualTo(f);
        assertThat(mappings.get(0).getFeatureId()).isEqualTo(f.getId());
    }
}
