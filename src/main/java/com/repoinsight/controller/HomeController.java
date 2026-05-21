package com.repoinsight.controller;

import com.repoinsight.service.ai.AiEnrichmentService;
import com.repoinsight.service.AnalysisOrchestrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home / landing page controller.
 */
@Controller
public class HomeController {

    private final AiEnrichmentService        aiEnrichment;
    private final AnalysisOrchestrationService orchestration;

    public HomeController(AiEnrichmentService aiEnrichment, AnalysisOrchestrationService orchestration) {
        this.aiEnrichment  = aiEnrichment;
        this.orchestration = orchestration;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("aiEnabled",    aiEnrichment.isEnabled());
        model.addAttribute("recentResults", orchestration.getAllResults());
        return "pages/home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("aiEnabled", aiEnrichment.isEnabled());
        return "pages/about";
    }
}
