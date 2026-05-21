package com.repoinsight.service.ai;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.RepositoryInsight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade service for optional AI enrichment of analysis results.
 * Falls back gracefully when AI is unavailable.
 * AI-enriched content is always clearly marked.
 */
@Service
public class AiEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(AiEnrichmentService.class);

    private static final String AI_LABEL = "[AI-assisted] ";

    private final AppProperties properties;
    private final AiProvider    aiProvider;

    public AiEnrichmentService(AppProperties properties, AiProvider aiProvider) {
        this.properties  = properties;
        this.aiProvider  = aiProvider;
    }

    /**
     * Returns true when AI enrichment is both configured and available.
     */
    public boolean isEnabled() {
        return properties.getAi().isEnabled() && aiProvider.isAvailable();
    }

    /**
     * Attempts to enrich a {@link RepositoryInsight} with AI-generated commentary.
     * Does nothing if AI is not enabled.
     */
    public void enrichInsight(RepositoryInsight insight) {
        if (!isEnabled()) return;

        log.info("AI enrichment enabled – enriching repository insight");

        try {
            String summaryPrompt =
                    "Provide a concise executive summary (3-4 sentences) for the following repository:\n"
                    + "Name: " + insight.getRepoName() + "\n"
                    + "Language: " + insight.getDominantLanguage() + "\n"
                    + "Detected modules: " + insight.getModules().stream()
                            .map(RepositoryInsight.ArchitectureNode::getName)
                            .toList() + "\n"
                    + "External integrations: " + insight.getExternalIntegrations() + "\n"
                    + "Respond only with the summary text.";

            String aiSummary = aiProvider.complete(summaryPrompt);
            if (!aiSummary.isBlank()) {
                insight.setExecutiveSummary(AI_LABEL + aiSummary);
                insight.setAiEnriched(true);
            }

            String domainPrompt =
                    "Based on the module names and integrations below, infer the business domain in 2 sentences:\n"
                    + "Modules: " + insight.getModules().stream()
                            .map(RepositoryInsight.ArchitectureNode::getName).toList() + "\n"
                    + "Integrations: " + insight.getExternalIntegrations();

            String aiDomain = aiProvider.complete(domainPrompt);
            if (!aiDomain.isBlank()) {
                insight.setDomainContext(AI_LABEL + aiDomain);
            }

        } catch (Exception e) {
            log.warn("AI enrichment failed: {} – falling back to deterministic output", e.getMessage());
        }
    }

    /**
     * Attempts to enhance a list of Gherkin scenarios with AI suggestions.
     */
    public List<String> enhanceGherkin(List<String> existing, String context) {
        if (!isEnabled()) return existing;
        try {
            String prompt =
                    "Enhance the following Gherkin scenarios for context '" + context + "':\n"
                    + String.join("\n", existing)
                    + "\nProvide improved scenarios with negative paths and edge cases. "
                    + "Format as Gherkin Feature/Scenario blocks.";
            String result = aiProvider.complete(prompt);
            if (!result.isBlank()) {
                return List.of(AI_LABEL + result);
            }
        } catch (Exception e) {
            log.warn("AI Gherkin enhancement failed: {}", e.getMessage());
        }
        return existing;
    }
}
