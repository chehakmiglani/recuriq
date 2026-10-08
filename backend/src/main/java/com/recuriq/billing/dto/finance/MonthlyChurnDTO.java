package com.recuriq.billing.dto.finance;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyChurnDTO {
    private String month;            // e.g. "2026-01"
    private int year;
    private BigDecimal netChurnPercent;
}
