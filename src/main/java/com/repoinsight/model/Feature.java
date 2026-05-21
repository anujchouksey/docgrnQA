package com.repoinsight.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an inferred functional feature or capability from the dev repo.
 */
public class Feature {

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }
    public enum Risk { CRITICAL, HIGH, MEDIUM, LOW }

    private String id;
    private String name;
    private String description;
    private String module;
    private String subModule;
    private String interfaceType;  // UI / API / JOB / EVENT / ADMIN
    private Severity severity;
    private Risk risk;
    private double confidence;     // 0.0 – 1.0
    private List<EvidenceLink> evidenceLinks = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private boolean aiEnriched;

    public Feature() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getSubModule() { return subModule; }
    public void setSubModule(String subModule) { this.subModule = subModule; }
    public String getInterfaceType() { return interfaceType; }
    public void setInterfaceType(String interfaceType) { this.interfaceType = interfaceType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<EvidenceLink> getEvidenceLinks() { return evidenceLinks; }
    public void setEvidenceLinks(List<EvidenceLink> evidenceLinks) { this.evidenceLinks = evidenceLinks; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public boolean isAiEnriched() { return aiEnriched; }
    public void setAiEnriched(boolean aiEnriched) { this.aiEnriched = aiEnriched; }
}
