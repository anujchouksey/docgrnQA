package com.repoinsight.controller;

import com.repoinsight.dto.AnalysisRequestDto;
import com.repoinsight.service.AnalysisOrchestrationService;
import com.repoinsight.service.ai.AiEnrichmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Handles the Repository Understanding analysis flow.
 */
@Controller
@RequestMapping("/understanding")
public class UnderstandingController {

    private final AnalysisOrchestrationService orchestration;
    private final AiEnrichmentService          aiEnrichment;

    public UnderstandingController(AnalysisOrchestrationService orchestration, AiEnrichmentService aiEnrichment) {
        this.orchestration = orchestration;
        this.aiEnrichment  = aiEnrichment;
    }

    /** Setup form for repository understanding analysis. */
    @GetMapping("/setup")
    public String setupForm(Model model) {
        model.addAttribute("request",   new AnalysisRequestDto());
        model.addAttribute("aiEnabled", aiEnrichment.isEnabled());
        return "pages/understanding-setup";
    }

    /** Submits the understanding analysis request. */
    @PostMapping("/analyze")
    public String analyze(@Valid @ModelAttribute("request") AnalysisRequestDto request,
                          BindingResult bindingResult,
                          Model model) {

        request.setMode(AnalysisRequestDto.Mode.UNDERSTANDING);

        if (bindingResult.hasErrors()) {
            model.addAttribute("aiEnabled", aiEnrichment.isEnabled());
            return "pages/understanding-setup";
        }

        String runId = orchestration.startAnalysis(request);
        return "redirect:/understanding/progress/" + runId;
    }

    /** Shows progress / polling page. */
    @GetMapping("/progress/{runId}")
    public String progress(@PathVariable String runId, Model model) {
        model.addAttribute("runId", runId);
        model.addAttribute("mode",  "UNDERSTANDING");
        return "pages/progress";
    }

    /** Shows the understanding report. */
    @GetMapping("/report/{runId}")
    public String report(@PathVariable String runId, Model model) {
        return orchestration.getResult(runId).map(result -> {
            model.addAttribute("result",    result);
            model.addAttribute("insight",   result.getRepositoryInsight());
            model.addAttribute("repo",      result.getTargetRepo());
            model.addAttribute("aiEnabled", result.isAiEnabled());
            return "pages/understanding-report";
        }).orElse("redirect:/");
    }
}
