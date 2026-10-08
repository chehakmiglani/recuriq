package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.request.*;
import com.recuriq.billing.service.CustomerSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSubscriptionController {

   private final CustomerSubscriptionService subscriptionService;

   @GetMapping("/subscription")
   public ResponseEntity<SubscriptionDTO> getCurrentSubscription(@AuthenticationPrincipal User user) {
       SubscriptionDTO subscription = subscriptionService.getCurrentSubscription(user.getEmail());
       return subscription != null ? ResponseEntity.ok(subscription) : ResponseEntity.notFound().build();
   }

   @PostMapping("/subscription")
   public ResponseEntity<SubscriptionDTO> createSubscription(
           @AuthenticationPrincipal User user,
           @RequestBody CreateSubscriptionRequest request) {
       return ResponseEntity.ok(subscriptionService.createSubscription(user.getEmail(), request));
   }

   @PutMapping("/subscription/upgrade")
   public ResponseEntity<SubscriptionDTO> upgradeSubscription(
           @AuthenticationPrincipal User user,
           @RequestBody UpgradeSubscriptionRequest request) {
       return ResponseEntity.ok(subscriptionService.upgradeSubscription(user.getEmail(), request));
   }

    @PostMapping("/subscription/cancel-request")
    public ResponseEntity<CancellationRequestDTO> createCancellationRequest(
            @AuthenticationPrincipal User user,
            @RequestBody CancellationRequestInput request) {
        return ResponseEntity.ok(subscriptionService.createCancellationRequest(user.getEmail(), request));
    }

    @PostMapping("/subscription/cancel-request/withdraw")
    public ResponseEntity<CancellationRequestDTO> withdrawCancellationRequest(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.withdrawCancellationRequest(user.getEmail()));
    }

    @GetMapping("/subscription/cancel-request")
    public ResponseEntity<CancellationRequestDTO> getPendingCancellationRequest(@AuthenticationPrincipal User user) {
        CancellationRequestDTO req = subscriptionService.getPendingCancellationRequest(user.getEmail());
        return req != null ? ResponseEntity.ok(req) : ResponseEntity.noContent().build();
    }

   @PutMapping("/subscription/pause")
   public ResponseEntity<SubscriptionDTO> pauseSubscription(
           @AuthenticationPrincipal User user,
           @RequestBody PauseSubscriptionRequest request) {
       return ResponseEntity.ok(subscriptionService.pauseSubscription(user.getEmail(), request));
   }

   @PutMapping("/subscription/resume")
   public ResponseEntity<SubscriptionDTO> resumeSubscription(@AuthenticationPrincipal User user) {
       return ResponseEntity.ok(subscriptionService.resumeSubscription(user.getEmail()));
   }

   @PostMapping("/addons/{addonId}")
   public ResponseEntity<SubscriptionDTO> addAddOn(
           @AuthenticationPrincipal User user,
           @PathVariable Long addonId) {
       return ResponseEntity.ok(subscriptionService.addAddOn(user.getEmail(), addonId));
   }

   @DeleteMapping("/addons/{addonId}")
   public ResponseEntity<SubscriptionDTO> removeAddOn(
           @AuthenticationPrincipal User user,
           @PathVariable Long addonId) {
       return ResponseEntity.ok(subscriptionService.removeAddOn(user.getEmail(), addonId));
   }

   @GetMapping("/usage")
   public ResponseEntity<List<UsageRecordDTO>> getMeteredUsage(
           @AuthenticationPrincipal User user,
           @RequestParam(required = false) String startDate,
           @RequestParam(required = false) String endDate) {
       return ResponseEntity.ok(subscriptionService.getMeteredUsage(user.getEmail(), startDate, endDate));
   }
}
