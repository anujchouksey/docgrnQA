package com.repoinsight.service.ai;

/**
 * Abstraction for AI provider enrichment.
 * Implement this interface to plug in any AI provider (OpenAI, Azure OpenAI, etc.)
 */
public interface AiProvider {

    /**
     * Returns true when the provider is configured and available.
     */
    boolean isAvailable();

    /**
     * Sends a prompt and returns the text response.
     *
     * @param prompt the prompt to send
     * @return AI response text, or an empty string if unavailable
     */
    String complete(String prompt);

    /**
     * Provider name for display purposes.
     */
    String getName();
}
