package com.repoinsight.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * No-op AI provider used when AI is disabled.
 * All operations return empty strings and {@link #isAvailable()} returns false.
 */
@Component
public class NoOpAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(NoOpAiProvider.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String complete(String prompt) {
        log.debug("AI is disabled – prompt ignored");
        return "";
    }

    @Override
    public String getName() {
        return "No-Op (AI disabled)";
    }
}
