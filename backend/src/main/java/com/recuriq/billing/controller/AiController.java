package com.recuriq.billing.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.dto.ai.InsightsDTO;
import com.recuriq.billing.service.AiAssistantService;
import com.recuriq.billing.service.ChurnRiskService;
import com.recuriq.billing.service.InsightsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final ChurnRiskService churnRiskService;
    private final InsightsService insightsService;

    /**
     * GET /api/ai/status
     * Lets the UI hide the assistant when no API key is configured.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'SUPPORT')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("assistantEnabled", aiAssistantService.isEnabled()));
    }

    /**
     * POST /api/ai/ask
     * Body: { "question": "...", "history": [{ "role": "user|assistant", "content": "..." }] }
     * Returns the assistant's answer and the data tools it used.
     */
    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<AssistantResponse> ask(@Valid @RequestBody AssistantRequest request) {
        return ResponseEntity.ok(aiAssistantService.ask(request));
    }

    /**
     * GET /api/ai/insights?refresh=false
     * MRR forecast, churn-risk highlights and a cached AI briefing for the finance overview.
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<InsightsDTO> getInsights(@RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(insightsService.getInsights(refresh));
    }

    /**
     * GET /api/ai/churn-risk?level=HIGH
     * Churn risk score and reasons for every customer with a live subscription.
     */
    @GetMapping("/churn-risk")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'SUPPORT')")
    public ResponseEntity<ChurnRiskSummaryDTO> getChurnRisk(@RequestParam(required = false) String level) {
        return ResponseEntity.ok(churnRiskService.getChurnRisk(level));
    }
}
