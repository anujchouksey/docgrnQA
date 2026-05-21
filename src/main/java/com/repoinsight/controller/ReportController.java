package com.repoinsight.controller;

import com.repoinsight.model.AnalysisResult;
import com.repoinsight.service.AnalysisOrchestrationService;
import com.repoinsight.service.reporting.ReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Handles report exports and status polling API.
 */
@Controller
@RequestMapping("/api")
public class ReportController {

    private final AnalysisOrchestrationService orchestration;
    private final ReportingService             reporting;

    public ReportController(AnalysisOrchestrationService orchestration, ReportingService reporting) {
        this.orchestration = orchestration;
        this.reporting     = reporting;
    }

    // ── Status polling ────────────────────────────────────────────────────────

    @GetMapping("/status/{runId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> status(@PathVariable String runId) {
        Optional<AnalysisResult> opt = orchestration.getResult(runId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AnalysisResult r = opt.get();
        Map<String, Object> body = Map.of(
                "status",        r.getStatus().name(),
                "message",       r.getStatusMessage() != null ? r.getStatusMessage() : "",
                "mode",          r.getMode() != null ? r.getMode().name() : "",
                "completed",     r.getStatus() == AnalysisResult.AnalysisStatus.COMPLETED,
                "failed",        r.getStatus() == AnalysisResult.AnalysisStatus.FAILED
        );
        return ResponseEntity.ok(body);
    }

    // ── HTML export ───────────────────────────────────────────────────────────

    @GetMapping("/export/html/{runId}")
    public ResponseEntity<byte[]> exportHtml(@PathVariable String runId) {
        Optional<AnalysisResult> opt = orchestration.getResult(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        AnalysisResult result = opt.get();
        String html = result.getMode() == AnalysisResult.AnalysisMode.COVERAGE
                ? reporting.generateCoverageHtml(result)
                : reporting.generateUnderstandingHtml(result);

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"repoinsight-report-" + runId + ".html\"")
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .contentLength(bytes.length)
                .body(bytes);
    }

    // ── JSON export ───────────────────────────────────────────────────────────

    @GetMapping("/export/json/{runId}")
    public ResponseEntity<byte[]> exportJson(@PathVariable String runId) {
        Optional<AnalysisResult> opt = orchestration.getResult(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        String json  = reporting.generateJson(opt.get());
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"repoinsight-report-" + runId + ".json\"")
                .contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
