package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrReportDTO {
    private long arrMinor;                        // current ARR in INR paise (MRR × 12)
    private List<MonthlyTrendDTO> arrTrend;       // all months from snapshots, valueMinor = ARR
    private List<PlanRevenueDTO> revenueByPlan;   // ARR contribution per plan
    private List<RegionRevenueDTO> revenueByRegion; // ARR contribution per region
}
