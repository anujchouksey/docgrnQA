package com.repoinsight.model;

/**
 * A link back to source-code evidence for a feature or test.
 */
public class EvidenceLink {

    public enum EvidenceType { FILE, CLASS, METHOD, ENDPOINT, SCENARIO, TAG }

    private String filePath;
    private String relativePath;
    private int lineNumber;
    private String snippet;
    private EvidenceType evidenceType;
    private String remoteUrl;   // clickable GitHub link if available

    public EvidenceLink() {}

    public EvidenceLink(String filePath, String relativePath, int lineNumber, EvidenceType evidenceType) {
        this.filePath = filePath;
        this.relativePath = relativePath;
        this.lineNumber = lineNumber;
        this.evidenceType = evidenceType;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public EvidenceType getEvidenceType() { return evidenceType; }
    public void setEvidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; }
    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    /** Returns a displayable short label for the evidence. */
    public String getDisplayLabel() {
        if (relativePath != null) {
            String base = relativePath.contains("/")
                    ? relativePath.substring(relativePath.lastIndexOf('/') + 1)
                    : relativePath;
            return lineNumber > 0 ? base + ":" + lineNumber : base;
        }
        return filePath != null ? filePath : "unknown";
    }
}
