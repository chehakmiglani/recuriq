package com.recuriq.billing.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnRiskSummaryDTO {
    private int highRisk;
    private int mediumRisk;
    private int lowRisk;
    private long monthlyRevenueAtRiskMinor;   // sum of monthly value for HIGH risk customers
    private List<ChurnRiskDTO> customers;     // sorted by score, highest first
}
