package com.repoinsight.model;

/**
 * Represents the source of a repository (local path or remote URL).
 */
public class RepoSource {

    public enum SourceType { LOCAL, REMOTE }

    private String id;
    private SourceType sourceType;
    private String path;           // local filesystem path (after cloning if remote)
    private String originalInput;  // what the user typed
    private String remoteUrl;
    private String branch;
    private String resolvedName;   // inferred project name
    private String language;       // dominant language
    private long fileSizeBytes;
    private int totalFiles;
    private boolean valid;
    private String validationError;

    public RepoSource() {}

    public RepoSource(SourceType sourceType, String originalInput) {
        this.sourceType = sourceType;
        this.originalInput = originalInput;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getOriginalInput() { return originalInput; }
    public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }
    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getResolvedName() { return resolvedName; }
    public void setResolvedName(String resolvedName) { this.resolvedName = resolvedName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }
}
