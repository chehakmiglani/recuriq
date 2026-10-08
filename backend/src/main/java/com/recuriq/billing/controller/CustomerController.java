package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.service.CustomerService;
import com.recuriq.billing.service.SubscriptionFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {
    private final CustomerService customerService;
    private final SubscriptionFlowService subscriptionFlowService;

    @GetMapping("/me")
    public ResponseEntity<CustomerProfileDTO> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerService.getProfile(user.getEmail()));
    }

    @PutMapping("/me")
    public ResponseEntity<CustomerProfileDTO> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody CustomerProfileDTO dto) {
        return ResponseEntity.ok(customerService.updateProfile(user.getEmail(), dto));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> getAvailablePlans(
            @RequestParam(required = false) String region,
            @AuthenticationPrincipal User user) {
        String resolvedRegion = resolveRegion(region, user);
        return ResponseEntity.ok(customerService.getAvailablePlans(resolvedRegion));
    }

    @GetMapping("/plans/featured")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<PlanDTO>> getFeaturedPlans(
            @RequestParam(required = false) String region,
            @AuthenticationPrincipal User user) {
        String resolvedRegion = resolveRegion(region, user);
        return ResponseEntity.ok(customerService.getFeaturedPlans(resolvedRegion));
    }

    @GetMapping("/plans/all")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<PlanDTO>> getAllPlans(
            @RequestParam(required = false) String region,
            @AuthenticationPrincipal User user) {
        String resolvedRegion = resolveRegion(region, user);
        return ResponseEntity.ok(customerService.getAllActivePlans(resolvedRegion));
    }

    private String resolveRegion(String requestedRegion, User user) {
        if (requestedRegion != null && !requestedRegion.trim().isEmpty()) {
            return requestedRegion;
        }
        if (user != null) {
            try {
                return customerService.getProfile(user.getEmail()).getCountry();
            } catch (Exception e) {
                // Ignore if customer profile not fully setup yet
            }
        }
        return "IN"; // Default fallback
    }

    @GetMapping("/addons")
    public ResponseEntity<List<AddOnDTO>> getAvailableAddOns(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerService.getAvailableAddOns(user.getEmail()));
    }

    // ========== SUBSCRIPTION FLOW ENDPOINTS ==========
    // Any signed-in user may ask; staff accounts simply get isCustomer = false
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerStatusResponse> checkCustomerStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionFlowService.checkCustomerStatus(user.getEmail()));
    }

    @PostMapping("/register-details")
    public ResponseEntity<Map<String, Object>> registerCustomerDetails(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CustomerRegistrationRequest request) {
        Customer customer = subscriptionFlowService.registerCustomerDetails(user.getEmail(), request);
        Map<String, Object> response = new HashMap<>();
        response.put("customerId", customer.getId());
        response.put("message", "Customer details registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment-method")
    public ResponseEntity<Map<String, Object>> createPaymentMethod(
            @RequestParam Long customerId,
            @Valid @RequestBody PaymentMethodRequest request) {
        PaymentMethod paymentMethod = subscriptionFlowService.createPaymentMethod(customerId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("paymentMethodId", paymentMethod.getId());
        response.put("last4", paymentMethod.getCardLast4());
        response.put("brand", paymentMethod.getCardBrand());
        response.put("message", "Payment method created successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscription/complete")
    public ResponseEntity<SubscriptionResponse> completeSubscription(
            @RequestParam Long customerId,
            @Valid @RequestBody SubscriptionCompletionRequest request) {
        SubscriptionResponse response = subscriptionFlowService.completeSubscription(customerId, request);
        return ResponseEntity.ok(response);
    }
}
