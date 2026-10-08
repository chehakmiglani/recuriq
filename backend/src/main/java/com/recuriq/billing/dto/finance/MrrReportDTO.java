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
public class MrrReportDTO {
    private long mrrMinor;                       // current MRR in INR paise
    private List<MonthlyTrendDTO> revenueTrend;  // all months from snapshots, valueMinor = MRR
}
