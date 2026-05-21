package com.repoinsight.service;

import com.repoinsight.dto.AnalysisRequestDto;
import com.repoinsight.model.*;
import com.repoinsight.service.ai.AiEnrichmentService;
import com.repoinsight.service.analysis.*;
import com.repoinsight.service.coverage.CoverageMappingService;
import com.repoinsight.service.coverage.CoverageScoringService;
import com.repoinsight.service.recommendation.RecommendationEngine;
import com.repoinsight.service.source.SourceIngestionService;
import com.repoinsight.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central orchestrator that drives a full analysis run end-to-end.
 */
@Service
public class AnalysisOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrationService.class);

    // In-memory store of completed results (keyed by run ID)
    private final Map<String, AnalysisResult> resultStore = new ConcurrentHashMap<>();

    private final SourceIngestionService          sourceIngestion;
    private final DevRepoAnalyzerService          devAnalyzer;
    private final QaRepoAnalyzerService           qaAnalyzer;
    private final CoverageMappingService          coverageMapper;
    private final CoverageScoringService          coverageScoring;
    private final RecommendationEngine            recommendationEngine;
    private final RepositoryUnderstandingService  understandingService;
    private final AiEnrichmentService             aiEnrichment;

    public AnalysisOrchestrationService(
            SourceIngestionService          sourceIngestion,
            DevRepoAnalyzerService          devAnalyzer,
            QaRepoAnalyzerService           qaAnalyzer,
            CoverageMappingService          coverageMapper,
            CoverageScoringService          coverageScoring,
            RecommendationEngine            recommendationEngine,
            RepositoryUnderstandingService  understandingService,
            AiEnrichmentService             aiEnrichment) {
        this.sourceIngestion      = sourceIngestion;
        this.devAnalyzer          = devAnalyzer;
        this.qaAnalyzer           = qaAnalyzer;
        this.coverageMapper       = coverageMapper;
        this.coverageScoring      = coverageScoring;
        this.recommendationEngine = recommendationEngine;
        this.understandingService = understandingService;
        this.aiEnrichment         = aiEnrichment;
    }

    /**
     * Starts an analysis run and returns the run ID immediately.
     * The actual work runs asynchronously.
     */
    public String startAnalysis(AnalysisRequestDto request) {
        String runId = PathUtil.newId();
        AnalysisResult placeholder = new AnalysisResult();
        placeholder.setId(runId);
        placeholder.setStatus(AnalysisResult.AnalysisStatus.IN_PROGRESS);
        placeholder.setStatusMessage("Analysis started...");
        placeholder.setStartedAt(Instant.now());
        placeholder.setMode(request.getMode() == AnalysisRequestDto.Mode.COVERAGE
                ? AnalysisResult.AnalysisMode.COVERAGE
                : AnalysisResult.AnalysisMode.UNDERSTANDING);
        placeholder.setAiEnabled(request.isAiEnabled());
        resultStore.put(runId, placeholder);

        runAsync(runId, request);
        return runId;
    }

    @Async
    public void runAsync(String runId, AnalysisRequestDto request) {
        AnalysisResult result = resultStore.get(runId);
        try {
            if (request.getMode() == AnalysisRequestDto.Mode.COVERAGE) {
                runCoverageAnalysis(result, request);
            } else {
                runUnderstandingAnalysis(result, request);
            }
            result.setStatus(AnalysisResult.AnalysisStatus.COMPLETED);
            result.setStatusMessage("Analysis complete");
        } catch (Exception e) {
            log.error("Analysis failed for run {}: {}", runId, e.getMessage(), e);
            result.setStatus(AnalysisResult.AnalysisStatus.FAILED);
            result.setStatusMessage("Analysis failed: " + e.getMessage());
        } finally {
            result.setCompletedAt(Instant.now());
        }
    }

    // ── Coverage Analysis ────────────────────────────────────────────────────

    private void runCoverageAnalysis(AnalysisResult result, AnalysisRequestDto req) {
        result.setStatusMessage("Resolving repositories...");

        RepoSource devRepo = sourceIngestion.resolveDevRepo(req);
        RepoSource qaRepo  = sourceIngestion.resolveQaRepo(req);
        result.setDevRepo(devRepo);
        result.setQaRepo(qaRepo);

        if (!devRepo.isValid()) {
            throw new IllegalArgumentException("Dev repo invalid: " + devRepo.getValidationError());
        }
        if (!qaRepo.isValid()) {
            throw new IllegalArgumentException("QA repo invalid: " + qaRepo.getValidationError());
        }

        result.setStatusMessage("Analyzing dev repository...");
        List<Feature> features = devAnalyzer.analyze(devRepo);
        result.setFeatures(features);

        result.setStatusMessage("Analyzing QA repository...");
        List<TestAsset> testAssets = qaAnalyzer.analyze(qaRepo);
        result.setTestAssets(testAssets);

        result.setStatusMessage("Computing coverage mappings...");
        List<CoverageMapping> mappings = coverageMapper.map(features, testAssets);
        result.setCoverageMappings(mappings);

        result.setStatusMessage("Generating recommendations...");
        List<Recommendation> recommendations = recommendationEngine.generate(mappings);
        result.setRecommendations(recommendations);

        result.setStatusMessage("Computing summary statistics...");
        result.setCoverageSummary(coverageScoring.computeSummary(mappings, recommendations, testAssets));

        result.setStatusMessage("Analysis complete");
        log.info("Coverage analysis complete for run {}: {} features, {} tests, {} recommendations",
                result.getId(), features.size(), testAssets.size(), recommendations.size());
    }

    // ── Understanding Analysis ────────────────────────────────────────────────

    private void runUnderstandingAnalysis(AnalysisResult result, AnalysisRequestDto req) {
        result.setStatusMessage("Resolving repository...");

        RepoSource targetRepo = sourceIngestion.resolveTargetRepo(req);
        result.setTargetRepo(targetRepo);

        if (!targetRepo.isValid()) {
            throw new IllegalArgumentException("Target repo invalid: " + targetRepo.getValidationError());
        }

        result.setStatusMessage("Analyzing repository structure...");
        RepositoryInsight insight = understandingService.analyze(targetRepo);

        if (result.isAiEnabled()) {
            result.setStatusMessage("Applying AI enrichment...");
            aiEnrichment.enrichInsight(insight);
        }

        result.setRepositoryInsight(insight);
        result.setStatusMessage("Analysis complete");
        log.info("Understanding analysis complete for run {}", result.getId());
    }

    // ── Result retrieval ──────────────────────────────────────────────────────

    public Optional<AnalysisResult> getResult(String runId) {
        return Optional.ofNullable(resultStore.get(runId));
    }

    public List<AnalysisResult> getAllResults() {
        return new ArrayList<>(resultStore.values());
    }

    public boolean hasResult(String runId) {
        return resultStore.containsKey(runId);
    }

    /**
     * Runs analysis synchronously (for testing and direct API calls).
     */
    public AnalysisResult runSync(AnalysisRequestDto request) {
        String runId = PathUtil.newId();
        AnalysisResult result = new AnalysisResult();
        result.setId(runId);
        result.setStartedAt(Instant.now());
        result.setMode(request.getMode() == AnalysisRequestDto.Mode.COVERAGE
                ? AnalysisResult.AnalysisMode.COVERAGE
                : AnalysisResult.AnalysisMode.UNDERSTANDING);
        result.setAiEnabled(request.isAiEnabled());
        result.setStatus(AnalysisResult.AnalysisStatus.IN_PROGRESS);

        try {
            if (request.getMode() == AnalysisRequestDto.Mode.COVERAGE) {
                runCoverageAnalysis(result, request);
            } else {
                runUnderstandingAnalysis(result, request);
            }
            result.setStatus(AnalysisResult.AnalysisStatus.COMPLETED);
        } catch (Exception e) {
            result.setStatus(AnalysisResult.AnalysisStatus.FAILED);
            result.setStatusMessage("Failed: " + e.getMessage());
        } finally {
            result.setCompletedAt(Instant.now());
            resultStore.put(runId, result);
        }

        return result;
    }
}
