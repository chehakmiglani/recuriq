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
public class ArpuLtvReportDTO {
    private long arpuMinor;                      // current ARPU in INR paise
    private long ltvMinor;                       // current LTV in INR paise
    private String cacLtvRatio;                  // e.g. "1:3.2"
    private List<MonthlyTrendDTO> arpuTrend;    // all months from snapshots, valueMinor = ARPU of that month
}
