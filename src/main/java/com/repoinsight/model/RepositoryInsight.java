package com.repoinsight.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A comprehensive understanding report for a single repository or folder.
 */
public class RepositoryInsight {

    private String repoName;
    private String repoPath;
    private String executiveSummary;
    private String systemPurpose;
    private String domainContext;
    private List<ArchitectureNode> modules = new ArrayList<>();
    private String architectureOverview;
    private String mermaidArchitectureDiagram;
    private String mermaidFlowDiagram;
    private String requestLifecycle;
    private String coreLogicExplanation;
    private List<String> externalIntegrations = new ArrayList<>();
    private List<String> securitySensitiveAreas = new ArrayList<>();
    private List<String> highRiskChangeAreas = new ArrayList<>();
    private List<String> errorPronePaths = new ArrayList<>();
    private String recommendedQaStrategy;
    private List<String> suggestedGherkinScenarios = new ArrayList<>();
    private List<String> edgeCases = new ArrayList<>();
    private String stakeholderExplanation;
    private String technicalExplanation;
    private List<String> missingDocumentation = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private String dominantLanguage;
    private int totalFiles;
    private boolean aiEnriched;
    private List<String> assumptions = new ArrayList<>();
    private List<String> limitations = new ArrayList<>();

    public RepositoryInsight() {}

    // ── Nested type ──────────────────────────────────────────────────────────

    public static class ArchitectureNode {
        private String name;
        private String role;
        private String description;
        private List<String> dependencies = new ArrayList<>();
        private List<EvidenceLink> evidenceLinks = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        public List<EvidenceLink> getEvidenceLinks() { return evidenceLinks; }
        public void setEvidenceLinks(List<EvidenceLink> evidenceLinks) { this.evidenceLinks = evidenceLinks; }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public String getSystemPurpose() { return systemPurpose; }
    public void setSystemPurpose(String systemPurpose) { this.systemPurpose = systemPurpose; }
    public String getDomainContext() { return domainContext; }
    public void setDomainContext(String domainContext) { this.domainContext = domainContext; }
    public List<ArchitectureNode> getModules() { return modules; }
    public void setModules(List<ArchitectureNode> modules) { this.modules = modules; }
    public String getArchitectureOverview() { return architectureOverview; }
    public void setArchitectureOverview(String architectureOverview) { this.architectureOverview = architectureOverview; }
    public String getMermaidArchitectureDiagram() { return mermaidArchitectureDiagram; }
    public void setMermaidArchitectureDiagram(String mermaidArchitectureDiagram) { this.mermaidArchitectureDiagram = mermaidArchitectureDiagram; }
    public String getMermaidFlowDiagram() { return mermaidFlowDiagram; }
    public void setMermaidFlowDiagram(String mermaidFlowDiagram) { this.mermaidFlowDiagram = mermaidFlowDiagram; }
    public String getRequestLifecycle() { return requestLifecycle; }
    public void setRequestLifecycle(String requestLifecycle) { this.requestLifecycle = requestLifecycle; }
    public String getCoreLogicExplanation() { return coreLogicExplanation; }
    public void setCoreLogicExplanation(String coreLogicExplanation) { this.coreLogicExplanation = coreLogicExplanation; }
    public List<String> getExternalIntegrations() { return externalIntegrations; }
    public void setExternalIntegrations(List<String> externalIntegrations) { this.externalIntegrations = externalIntegrations; }
    public List<String> getSecuritySensitiveAreas() { return securitySensitiveAreas; }
    public void setSecuritySensitiveAreas(List<String> securitySensitiveAreas) { this.securitySensitiveAreas = securitySensitiveAreas; }
    public List<String> getHighRiskChangeAreas() { return highRiskChangeAreas; }
    public void setHighRiskChangeAreas(List<String> highRiskChangeAreas) { this.highRiskChangeAreas = highRiskChangeAreas; }
    public List<String> getErrorPronePaths() { return errorPronePaths; }
    public void setErrorPronePaths(List<String> errorPronePaths) { this.errorPronePaths = errorPronePaths; }
    public String getRecommendedQaStrategy() { return recommendedQaStrategy; }
    public void setRecommendedQaStrategy(String recommendedQaStrategy) { this.recommendedQaStrategy = recommendedQaStrategy; }
    public List<String> getSuggestedGherkinScenarios() { return suggestedGherkinScenarios; }
    public void setSuggestedGherkinScenarios(List<String> suggestedGherkinScenarios) { this.suggestedGherkinScenarios = suggestedGherkinScenarios; }
    public List<String> getEdgeCases() { return edgeCases; }
    public void setEdgeCases(List<String> edgeCases) { this.edgeCases = edgeCases; }
    public String getStakeholderExplanation() { return stakeholderExplanation; }
    public void setStakeholderExplanation(String stakeholderExplanation) { this.stakeholderExplanation = stakeholderExplanation; }
    public String getTechnicalExplanation() { return technicalExplanation; }
    public void setTechnicalExplanation(String technicalExplanation) { this.technicalExplanation = technicalExplanation; }
    public List<String> getMissingDocumentation() { return missingDocumentation; }
    public void setMissingDocumentation(List<String> missingDocumentation) { this.missingDocumentation = missingDocumentation; }
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    public String getDominantLanguage() { return dominantLanguage; }
    public void setDominantLanguage(String dominantLanguage) { this.dominantLanguage = dominantLanguage; }
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    public boolean isAiEnriched() { return aiEnriched; }
    public void setAiEnriched(boolean aiEnriched) { this.aiEnriched = aiEnriched; }
    public List<String> getAssumptions() { return assumptions; }
    public void setAssumptions(List<String> assumptions) { this.assumptions = assumptions; }
    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }
}
