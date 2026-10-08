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
public class ChurnReportDTO {
    private BigDecimal netChurnPercent;       // overall net churn %
    private BigDecimal revenueChurnPercent;   // revenue lost from canceled subs as % of total MRR pool
    private long churnedRevenueMinor;     // total monthly revenue lost from CANCELED subs, INR paise
    private List<MonthlyChurnDTO> churnTrend; // month-by-month net churn % from snapshots
    private List<ChurnReasonDTO> reasons;        // breakdown of churn reasons
}
