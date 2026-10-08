package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSnapshotDTO {
    // from revenue_snapshot.snapshot_date
    private LocalDate date;

    // from revenue_snapshot.total_revenue_minor
    private long totalRevenueMinor;

    // from revenue_snapshot.mrr_minor
    private long mrrMinor;

    // from revenue_snapshot.arr_minor
    private long arrMinor;

    // from revenue_snapshot.active_customers
    private int activeCustomers;

    // from revenue_snapshot.new_customers
    private int newCustomers;

    // from revenue_snapshot.net_churn_percent
    private BigDecimal netChurnPercent;
}
