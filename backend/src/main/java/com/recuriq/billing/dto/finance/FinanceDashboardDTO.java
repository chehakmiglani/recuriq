package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDashboardDTO {
    // Core KPIs — all monetary values in INR paise (minor units)
    private long mrrMinor;
    private long arrMinor;
    private long arpuMinor;
    private long ltvMinor;
    private BigDecimal netChurnPercent;  // percentage, e.g. 5.26
    private int activeCustomers;
    private long failedPaymentsCount;
    private long refundAmountMinor;

    // Breakdowns
    private List<PlanRevenueDTO> revenueByPlan;      // MRR contribution per plan
    private List<RegionRevenueDTO> revenueByRegion;  // MRR contribution per region (IN/US/GB)

    // Trend — one entry per snapshot month, valueMinor = MRR of that month
    private List<MonthlyTrendDTO> revenueTrend;
}
