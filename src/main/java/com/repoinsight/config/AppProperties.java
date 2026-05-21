package com.repoinsight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "repoinsight")
public class AppProperties {

    private Workspace workspace = new Workspace();
    private Analysis analysis = new Analysis();
    private Coverage coverage = new Coverage();
    private Ai ai = new Ai();

    // ── Workspace ────────────────────────────────────────────────────────────

    public static class Workspace {
        private String baseDir = System.getProperty("java.io.tmpdir") + "/repoinsight-workspace";
        private boolean cleanupOnExit = true;

        public String getBaseDir() { return baseDir; }
        public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
        public boolean isCleanupOnExit() { return cleanupOnExit; }
        public void setCleanupOnExit(boolean cleanupOnExit) { this.cleanupOnExit = cleanupOnExit; }
    }

    // ── Analysis ─────────────────────────────────────────────────────────────

    public static class Analysis {
        private int maxFileSizeKb = 512;
        private int maxFilesPerRepo = 5000;
        private List<String> includeExtensions = List.of(
                ".java", ".kt", ".feature", ".groovy", ".js", ".ts",
                ".py", ".go", ".rb", ".xml", ".yaml", ".yml", ".json", ".properties");
        private List<String> excludeDirs = List.of(
                ".git", "node_modules", "target", "build", "dist", ".gradle", ".idea", ".vscode");

        public int getMaxFileSizeKb() { return maxFileSizeKb; }
        public void setMaxFileSizeKb(int maxFileSizeKb) { this.maxFileSizeKb = maxFileSizeKb; }
        public int getMaxFilesPerRepo() { return maxFilesPerRepo; }
        public void setMaxFilesPerRepo(int maxFilesPerRepo) { this.maxFilesPerRepo = maxFilesPerRepo; }
        public List<String> getIncludeExtensions() { return includeExtensions; }
        public void setIncludeExtensions(List<String> includeExtensions) { this.includeExtensions = includeExtensions; }
        public List<String> getExcludeDirs() { return excludeDirs; }
        public void setExcludeDirs(List<String> excludeDirs) { this.excludeDirs = excludeDirs; }
    }

    // ── Coverage ─────────────────────────────────────────────────────────────

    public static class Coverage {
        private Scoring scoring = new Scoring();

        public static class Scoring {
            private double exactMatchWeight = 1.0;
            private double tokenOverlapWeight = 0.8;
            private double pathAlignmentWeight = 0.6;
            private double fuzzyWeight = 0.5;
            private double tagMatchWeight = 0.9;
            private double minCoveredThreshold = 0.7;
            private double minPartialThreshold = 0.3;

            public double getExactMatchWeight() { return exactMatchWeight; }
            public void setExactMatchWeight(double v) { this.exactMatchWeight = v; }
            public double getTokenOverlapWeight() { return tokenOverlapWeight; }
            public void setTokenOverlapWeight(double v) { this.tokenOverlapWeight = v; }
            public double getPathAlignmentWeight() { return pathAlignmentWeight; }
            public void setPathAlignmentWeight(double v) { this.pathAlignmentWeight = v; }
            public double getFuzzyWeight() { return fuzzyWeight; }
            public void setFuzzyWeight(double v) { this.fuzzyWeight = v; }
            public double getTagMatchWeight() { return tagMatchWeight; }
            public void setTagMatchWeight(double v) { this.tagMatchWeight = v; }
            public double getMinCoveredThreshold() { return minCoveredThreshold; }
            public void setMinCoveredThreshold(double v) { this.minCoveredThreshold = v; }
            public double getMinPartialThreshold() { return minPartialThreshold; }
            public void setMinPartialThreshold(double v) { this.minPartialThreshold = v; }
        }

        public Scoring getScoring() { return scoring; }
        public void setScoring(Scoring scoring) { this.scoring = scoring; }
    }

    // ── AI ───────────────────────────────────────────────────────────────────

    public static class Ai {
        private boolean enabled = false;
        private String provider = "none";
        private OpenAi openai = new OpenAi();

        public static class OpenAi {
            private String apiKey;
            private String model = "gpt-4o";
            private String baseUrl = "https://api.openai.com/v1";
            private int maxTokens = 2048;
            private double temperature = 0.3;

            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public OpenAi getOpenai() { return openai; }
        public void setOpenai(OpenAi openai) { this.openai = openai; }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public Analysis getAnalysis() { return analysis; }
    public void setAnalysis(Analysis analysis) { this.analysis = analysis; }
    public Coverage getCoverage() { return coverage; }
    public void setCoverage(Coverage coverage) { this.coverage = coverage; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }
}
