package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDTO {
    private String month;   // e.g. "2026-01"
    private int year;
    private long valueMinor; // MRR minor for revenue trend, ARPU minor for ARPU trend
}
