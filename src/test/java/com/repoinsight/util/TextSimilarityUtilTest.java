package com.repoinsight.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class TextSimilarityUtilTest {

    private TextSimilarityUtil util;

    @BeforeEach
    void setUp() {
        util = new TextSimilarityUtil();
    }

    @Test
    void normalize_handlesNull() {
        assertThat(util.normalize(null)).isEqualTo("");
    }

    @Test
    void normalize_splitsCamelCase() {
        String result = util.normalize("createUserAccount");
        assertThat(result).contains("create").contains("user").contains("account");
    }

    @Test
    void normalize_removesStopWords() {
        String result = util.normalize("test the given when then");
        assertThat(result).doesNotContain("the");
    }

    @Test
    void tokenize_returnsTokenSet() {
        Set<String> tokens = util.tokenize("createUserAccount");
        assertThat(tokens).isNotEmpty();
    }

    @Test
    void tokenOverlap_identical_returnsHigh() {
        double score = util.tokenOverlap("create user account", "create user account");
        assertThat(score).isGreaterThan(0.8);
    }

    @Test
    void tokenOverlap_disjoint_returnsLow() {
        double score = util.tokenOverlap("apple banana cherry", "dog elephant fish");
        assertThat(score).isLessThan(0.2);
    }

    @Test
    void tokenOverlap_partial_returnsMid() {
        double score = util.tokenOverlap("create user account", "delete user record");
        // "user" overlaps
        assertThat(score).isBetween(0.05, 0.9);
    }

    @Test
    void exactMatch_sameAfterNormalization() {
        assertThat(util.exactMatch("createUser", "create user")).isTrue();
    }

    @Test
    void exactMatch_differentStrings_returnsFalse() {
        assertThat(util.exactMatch("createUser", "deleteAccount")).isFalse();
    }

    @Test
    void fuzzyMatch_identical_returnsHighScore() {
        double score = util.fuzzyMatch("user login", "user login");
        assertThat(score).isGreaterThan(0.8);
    }

    @Test
    void combinedSimilarity_identical_returnsHigh() {
        double score = util.combinedSimilarity("payment processing", "payment processing");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void combinedSimilarity_unrelated_returnsLow() {
        double score = util.combinedSimilarity("user registration flow", "database backup schedule");
        assertThat(score).isLessThan(0.5);
    }

    @Test
    void containsTokens_allTokensPresent() {
        assertThat(util.containsTokens("user login", "user login authentication flow")).isTrue();
    }

    @Test
    void containsTokens_missingTokens_returnsFalse() {
        assertThat(util.containsTokens("payment refund", "user login authentication")).isFalse();
    }
}
