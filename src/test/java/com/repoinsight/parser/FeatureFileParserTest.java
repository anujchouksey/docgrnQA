package com.repoinsight.parser;

import com.repoinsight.model.TestAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FeatureFileParserTest {

    private FeatureFileParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new FeatureFileParser();
    }

    private Path writeFeature(String content) throws IOException {
        Path file = tempDir.resolve("login.feature");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void parse_extractsFeatureTitle() throws IOException {
        String content = """
                Feature: User Login
                  Scenario: Successful login
                    Given a registered user
                    When they provide valid credentials
                    Then they are logged in
                """;
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getTitle()).isEqualTo("User Login");
    }

    @Test
    void parse_extractsScenarioTitles() throws IOException {
        String content = """
                Feature: User Login
                  Scenario: Successful login
                    Given a user exists
                    When they login
                    Then access is granted
                  Scenario: Failed login
                    Given a user exists
                    When they provide wrong password
                    Then access is denied
                """;
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getScenarioTitles()).hasSize(2);
        assertThat(asset.getScenarioTitles()).contains("Successful login", "Failed login");
    }

    @Test
    void parse_extractsTags() throws IOException {
        String content = """
                @smoke @regression
                Feature: User Login
                  @auth
                  Scenario: Login with valid credentials
                    Given a valid user
                    When they log in
                    Then success
                """;
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getTags()).contains("smoke", "regression", "auth");
    }

    @Test
    void parse_setsTestTypeToFeatureFile() throws IOException {
        String content = "Feature: Payments\n  Scenario: Pay invoice\n    Given an invoice\n    When paid\n    Then confirmed\n";
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getTestType()).isEqualTo(TestAsset.TestType.FEATURE_FILE);
    }

    @Test
    void parse_classifiesNegativeIntent() throws IOException {
        String content = """
                @negative
                Feature: Login Error Handling
                  Scenario: Login with invalid credentials fails
                    Given an invalid user
                    When they attempt to log in with wrong password
                    Then an error message is shown
                """;
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getIntentType()).isEqualTo(TestAsset.IntentType.NEGATIVE);
    }

    @Test
    void parse_classifiesSecurityIntent() throws IOException {
        String content = """
                @security @auth
                Feature: Authorization
                  Scenario: Unauthorized access is denied
                    Given an unauthenticated user
                    When they access a protected resource
                    Then they are redirected to login
                """;
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getIntentType()).isEqualTo(TestAsset.IntentType.SECURITY_AUTH);
    }

    @Test
    void parse_setsEvidenceLinks() throws IOException {
        String content = "Feature: Orders\n  Scenario: Place order\n    Given cart\n    When checkout\n    Then order placed\n";
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getEvidenceLinks()).isNotEmpty();
    }

    @Test
    void parse_fallsBackToFilenameWhenNoFeatureTitle() throws IOException {
        String content = "  Scenario: Something happens\n    Given a thing\n    When it runs\n    Then it works\n";
        Path file = writeFeature(content);
        TestAsset asset = parser.parse(file, tempDir, content);

        assertThat(asset.getTitle()).isNotBlank();
    }
}
