package com.recuriq.billing.dto.finance;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import com.recuriq.billing.entity.RevenueSnapshot;

@Data
@Builder
public class FinanceStatsResponse {
    private Long mrrMinor;
    private Long arrMinor;
    private Long arpuMinor;
    private Long ltvMinor;
    private Double churnRate;
    private Integer totalInvoices;
    private Integer paidInvoices;
    private Integer pendingInvoices;
    private Integer failedInvoices;
    private Long totalCollectedMinor;
    private Long pendingCollectionMinor;
    private List<RevenueSnapshot> recentSnapshots;
}
