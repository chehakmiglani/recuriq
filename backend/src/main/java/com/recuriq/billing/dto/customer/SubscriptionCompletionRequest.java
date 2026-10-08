package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.BillingPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionCompletionRequest {
    
    @NotNull(message = "Plan ID is required")
    private Long planId;
    
    private Long paymentMethodId;

    @Valid
    private PaymentMethodRequest paymentMethod;
    
    @NotNull(message = "Billing period is required")
    private BillingPeriod billingPeriod;

    private String couponCode;  // Optional coupon code
}
