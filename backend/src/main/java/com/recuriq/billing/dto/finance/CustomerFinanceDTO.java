package com.recuriq.billing.dto.finance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerFinanceDTO {
    private Long customerId;
    private String fullName;
    private String email;
    private String country;
    private String currency;
    private Integer activeSubscriptionsCount;
    private Long monthlyContributionMinor;
}
