package com.recuriq.billing.dto.finance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionFinanceDTO {
    private Long subscriptionId;
    private String customerName;
    private String customerEmail;
    private String planName;
    private String billingPeriod;
    private Long monthlyValueMinor;
    private Long annualValueMinor;
    private String currency;
    private String status;
    private String startDate;
}
