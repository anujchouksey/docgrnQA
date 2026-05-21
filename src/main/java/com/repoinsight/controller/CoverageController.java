package com.repoinsight.controller;

import com.repoinsight.dto.AnalysisRequestDto;
import com.repoinsight.service.AnalysisOrchestrationService;
import com.repoinsight.service.ai.AiEnrichmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the Coverage Intelligence analysis flow.
 */
@Controller
@RequestMapping("/coverage")
public class CoverageController {

    private final AnalysisOrchestrationService orchestration;
    private final AiEnrichmentService          aiEnrichment;

    public CoverageController(AnalysisOrchestrationService orchestration, AiEnrichmentService aiEnrichment) {
        this.orchestration = orchestration;
        this.aiEnrichment  = aiEnrichment;
    }

    /** Setup form for coverage analysis. */
    @GetMapping("/setup")
    public String setupForm(Model model) {
        model.addAttribute("request",   new AnalysisRequestDto());
        model.addAttribute("aiEnabled", aiEnrichment.isEnabled());
        return "pages/coverage-setup";
    }

    /** Submits the coverage analysis request. */
    @PostMapping("/analyze")
    public String analyze(@Valid @ModelAttribute("request") AnalysisRequestDto request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {

        // Force mode
        request.setMode(AnalysisRequestDto.Mode.COVERAGE);

        if (bindingResult.hasErrors()) {
            model.addAttribute("aiEnabled", aiEnrichment.isEnabled());
            return "pages/coverage-setup";
        }

        String runId = orchestration.startAnalysis(request);
        return "redirect:/coverage/progress/" + runId;
    }

    /** Shows progress / polling page. */
    @GetMapping("/progress/{runId}")
    public String progress(@PathVariable String runId, Model model) {
        model.addAttribute("runId", runId);
        model.addAttribute("mode",  "COVERAGE");
        return "pages/progress";
    }

    /** Shows the coverage results dashboard. */
    @GetMapping("/report/{runId}")
    public String report(@PathVariable String runId, Model model) {
        return orchestration.getResult(runId).map(result -> {
            model.addAttribute("result",          result);
            model.addAttribute("summary",         result.getCoverageSummary());
            model.addAttribute("mappings",        result.getCoverageMappings());
            model.addAttribute("recommendations", result.getRecommendations());
            model.addAttribute("features",        result.getFeatures());
            model.addAttribute("testAssets",      result.getTestAssets());
            model.addAttribute("aiEnabled",       result.isAiEnabled());
            return "pages/coverage-report";
        }).orElse("redirect:/");
    }
}
