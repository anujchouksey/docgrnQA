package com.repoinsight.service.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoinsight.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Generates exportable reports (HTML and JSON) from an {@link AnalysisResult}.
 */
@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final TemplateEngine templateEngine;
    private final ObjectMapper   objectMapper;

    public ReportingService(TemplateEngine templateEngine, ObjectMapper objectMapper) {
        this.templateEngine = templateEngine;
        this.objectMapper   = objectMapper;
    }

    /**
     * Renders the full standalone HTML report for a coverage analysis.
     */
    public String generateCoverageHtml(AnalysisResult result) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("result",       result);
        ctx.setVariable("summary",      result.getCoverageSummary());
        ctx.setVariable("mappings",     result.getCoverageMappings());
        ctx.setVariable("recommendations", result.getRecommendations());
        ctx.setVariable("features",     result.getFeatures());
        ctx.setVariable("testAssets",   result.getTestAssets());
        ctx.setVariable("devRepo",      result.getDevRepo());
        ctx.setVariable("qaRepo",       result.getQaRepo());
        try {
            return templateEngine.process("reports/coverage-export", ctx);
        } catch (Exception e) {
            log.error("Failed to generate coverage HTML: {}", e.getMessage(), e);
            return "<html><body><h1>Report generation failed: " + e.getMessage() + "</h1></body></html>";
        }
    }

    /**
     * Renders the full standalone HTML report for a repository understanding analysis.
     */
    public String generateUnderstandingHtml(AnalysisResult result) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("result",  result);
        ctx.setVariable("insight", result.getRepositoryInsight());
        ctx.setVariable("repo",    result.getTargetRepo());
        try {
            return templateEngine.process("reports/understanding-export", ctx);
        } catch (Exception e) {
            log.error("Failed to generate understanding HTML: {}", e.getMessage(), e);
            return "<html><body><h1>Report generation failed: " + e.getMessage() + "</h1></body></html>";
        }
    }

    /**
     * Serialises the analysis result to JSON.
     */
    public String generateJson(AnalysisResult result) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to generate JSON: {}", e.getMessage(), e);
            return "{\"error\": \"JSON generation failed: " + e.getMessage() + "\"}";
        }
    }
}
