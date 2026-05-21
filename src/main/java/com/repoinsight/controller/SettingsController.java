package com.repoinsight.controller;

import com.repoinsight.config.AppProperties;
import com.repoinsight.service.ai.AiEnrichmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Settings page controller.
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final AppProperties       properties;
    private final AiEnrichmentService aiEnrichment;

    public SettingsController(AppProperties properties, AiEnrichmentService aiEnrichment) {
        this.properties   = properties;
        this.aiEnrichment = aiEnrichment;
    }

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("properties",  properties);
        model.addAttribute("aiEnabled",   aiEnrichment.isEnabled());
        model.addAttribute("aiProvider",  properties.getAi().getProvider());
        model.addAttribute("scoring",     properties.getCoverage().getScoring());
        return "pages/settings";
    }
}
