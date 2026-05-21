package com.repoinsight.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A test asset extracted from the QA / BDD repository.
 */
public class TestAsset {

    public enum TestType {
        FEATURE_FILE, STEP_DEFINITION, PAGE_OBJECT, API_HELPER, UNIT_TEST, INTEGRATION_TEST, UNKNOWN
    }

    public enum IntentType {
        HAPPY_PATH, NEGATIVE, BOUNDARY, SECURITY_AUTH, INTEGRATION, ERROR_HANDLING, UNKNOWN
    }

    private String id;
    private String title;
    private String module;
    private TestType testType;
    private IntentType intentType;
    private String filePath;
    private String relativePath;
    private List<String> tags = new ArrayList<>();
    private List<String> scenarioTitles = new ArrayList<>();
    private List<String> stepKeywords = new ArrayList<>();
    private List<EvidenceLink> evidenceLinks = new ArrayList<>();

    public TestAsset() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public TestType getTestType() { return testType; }
    public void setTestType(TestType testType) { this.testType = testType; }
    public IntentType getIntentType() { return intentType; }
    public void setIntentType(IntentType intentType) { this.intentType = intentType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getScenarioTitles() { return scenarioTitles; }
    public void setScenarioTitles(List<String> scenarioTitles) { this.scenarioTitles = scenarioTitles; }
    public List<String> getStepKeywords() { return stepKeywords; }
    public void setStepKeywords(List<String> stepKeywords) { this.stepKeywords = stepKeywords; }
    public List<EvidenceLink> getEvidenceLinks() { return evidenceLinks; }
    public void setEvidenceLinks(List<EvidenceLink> evidenceLinks) { this.evidenceLinks = evidenceLinks; }
}
