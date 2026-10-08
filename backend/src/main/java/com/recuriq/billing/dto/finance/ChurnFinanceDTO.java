package com.recuriq.billing.dto.finance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChurnFinanceDTO {
    private Long subscriptionId;
    private String customerName;
    private String customerEmail;
    private String planName;
    private Long lostMonthlyRevenueMinor;
    private String currency;
    private String canceledAt;
    private String reason;
}
