package com.recuriq.billing.controller;

import com.recuriq.billing.dto.support.CustomerSearchResponse;
import com.recuriq.billing.dto.support.CustomerDetailResponse;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.entity.BillingJob;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.request.ProcessCancellationRequestInput;
import com.recuriq.billing.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPORT')")
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerSearchResponse>> searchCustomers(@RequestParam String query) {
        return ResponseEntity.ok(supportService.searchCustomers(query));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerDetails(@PathVariable Long id) {
        return ResponseEntity.ok(supportService.getCustomerDetails(id));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs() {
        return ResponseEntity.ok(supportService.getRecentAuditLogs());
    }

    @GetMapping("/billing-jobs")
    public ResponseEntity<List<BillingJob>> getBillingJobs() {
        return ResponseEntity.ok(supportService.getBillingJobs());
    }

    @GetMapping("/dunning-logs")
    public ResponseEntity<List<DunningRetryLog>> getDunningLogs() {
        return ResponseEntity.ok(supportService.getDunningLogs());
    }

    @GetMapping("/subscriptions/past-due")
    public ResponseEntity<List<com.recuriq.billing.dto.customer.SubscriptionDTO>> getPastDueSubscriptions() {
        return ResponseEntity.ok(supportService.getPastDueSubscriptions());
    }

    @GetMapping("/cancellation-requests")
    public ResponseEntity<List<CancellationRequestDTO>> getPendingCancellationRequests() {
        return ResponseEntity.ok(supportService.getPendingCancellationRequests());
    }

    @PostMapping("/cancellation-requests/{id}/approve")
    public ResponseEntity<CancellationResponse> approveCancellationRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User agent,
            @RequestBody ProcessCancellationRequestInput input) {
        return ResponseEntity.ok(supportService.approveCancellationRequest(id, agent.getEmail(), input));
    }

    @PostMapping("/cancellation-requests/{id}/reject")
    public ResponseEntity<Void> rejectCancellationRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User agent,
            @RequestBody ProcessCancellationRequestInput input) {
        supportService.rejectCancellationRequest(id, agent.getEmail(), input);
        return ResponseEntity.noContent().build();
    }
}
