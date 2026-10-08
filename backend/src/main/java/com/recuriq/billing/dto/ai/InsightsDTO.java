package com.recuriq.billing.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsDTO {

    // MRR forecast for the next months, minor units
    private List<ForecastPoint> mrrForecast;
    private long currentMrrMinor;
    private Double projectedGrowthPercent;   // change from current MRR to the last forecast month; null when no forecast

    // Churn risk at a glance
    private int highRisk;
    private int mediumRisk;
    private long monthlyRevenueAtRiskMinor;
    private List<ChurnRiskDTO> topRisks;     // up to 3 customers with the highest scores

    // Written briefing from the AI analyst; null when the assistant is not configured
    private String aiBriefing;
    private LocalDateTime aiBriefingGeneratedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private String month;      // e.g. "2026-11"
        private long valueMinor;
    }
}
