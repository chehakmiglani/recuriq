package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRevenueDTO {
    private Long planId;
    private String planName;
    private long revenueMinor; // MRR or ARR contribution in INR paise
}
