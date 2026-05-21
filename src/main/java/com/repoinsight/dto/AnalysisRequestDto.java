package com.repoinsight.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Form/request DTO for submitting an analysis request from the UI.
 */
public class AnalysisRequestDto {

    public enum Mode { COVERAGE, UNDERSTANDING }
    public enum SourceType { LOCAL, REMOTE }

    @NotNull(message = "Analysis mode is required")
    private Mode mode;

    private boolean aiEnabled = false;

    // ── Dev repo (coverage mode) ──────────────────────────────────────────────

    private SourceType devSourceType;
    private String devLocalPath;
    private String devRemoteUrl;
    private String devBranch;

    // ── QA repo (coverage mode) ───────────────────────────────────────────────

    private SourceType qaSourceType;
    private String qaLocalPath;
    private String qaRemoteUrl;
    private String qaBranch;

    // ── Target repo (understanding mode) ─────────────────────────────────────

    private SourceType targetSourceType;
    private String targetLocalPath;
    private String targetRemoteUrl;
    private String targetBranch;

    // ── Scope / filter ───────────────────────────────────────────────────────

    private String scopeFilter;
    private String outputFormat = "HTML";

    // ── Custom validation ─────────────────────────────────────────────────────

    @AssertTrue(message = "Dev and QA repo inputs are required for Coverage mode")
    public boolean isCoverageInputsValid() {
        if (mode != Mode.COVERAGE) return true;
        boolean devOk = (devSourceType == SourceType.LOCAL && devLocalPath != null && !devLocalPath.isBlank())
                || (devSourceType == SourceType.REMOTE && devRemoteUrl != null && !devRemoteUrl.isBlank());
        boolean qaOk = (qaSourceType == SourceType.LOCAL && qaLocalPath != null && !qaLocalPath.isBlank())
                || (qaSourceType == SourceType.REMOTE && qaRemoteUrl != null && !qaRemoteUrl.isBlank());
        return devOk && qaOk;
    }

    @AssertTrue(message = "Target repo input is required for Understanding mode")
    public boolean isUnderstandingInputsValid() {
        if (mode != Mode.UNDERSTANDING) return true;
        return (targetSourceType == SourceType.LOCAL && targetLocalPath != null && !targetLocalPath.isBlank())
                || (targetSourceType == SourceType.REMOTE && targetRemoteUrl != null && !targetRemoteUrl.isBlank());
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
    public SourceType getDevSourceType() { return devSourceType; }
    public void setDevSourceType(SourceType devSourceType) { this.devSourceType = devSourceType; }
    public String getDevLocalPath() { return devLocalPath; }
    public void setDevLocalPath(String devLocalPath) { this.devLocalPath = devLocalPath; }
    public String getDevRemoteUrl() { return devRemoteUrl; }
    public void setDevRemoteUrl(String devRemoteUrl) { this.devRemoteUrl = devRemoteUrl; }
    public String getDevBranch() { return devBranch; }
    public void setDevBranch(String devBranch) { this.devBranch = devBranch; }
    public SourceType getQaSourceType() { return qaSourceType; }
    public void setQaSourceType(SourceType qaSourceType) { this.qaSourceType = qaSourceType; }
    public String getQaLocalPath() { return qaLocalPath; }
    public void setQaLocalPath(String qaLocalPath) { this.qaLocalPath = qaLocalPath; }
    public String getQaRemoteUrl() { return qaRemoteUrl; }
    public void setQaRemoteUrl(String qaRemoteUrl) { this.qaRemoteUrl = qaRemoteUrl; }
    public String getQaBranch() { return qaBranch; }
    public void setQaBranch(String qaBranch) { this.qaBranch = qaBranch; }
    public SourceType getTargetSourceType() { return targetSourceType; }
    public void setTargetSourceType(SourceType targetSourceType) { this.targetSourceType = targetSourceType; }
    public String getTargetLocalPath() { return targetLocalPath; }
    public void setTargetLocalPath(String targetLocalPath) { this.targetLocalPath = targetLocalPath; }
    public String getTargetRemoteUrl() { return targetRemoteUrl; }
    public void setTargetRemoteUrl(String targetRemoteUrl) { this.targetRemoteUrl = targetRemoteUrl; }
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
    public String getScopeFilter() { return scopeFilter; }
    public void setScopeFilter(String scopeFilter) { this.scopeFilter = scopeFilter; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
}
