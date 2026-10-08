package com.recuriq.billing.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long subscriptionId;
    private Long invoiceId;
    private String invoiceNumber;
    private String status;
    private String message;
    private String trialEndDate;
    private Long totalAmountMinor;
}
