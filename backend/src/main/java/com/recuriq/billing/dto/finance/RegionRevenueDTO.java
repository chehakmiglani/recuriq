package com.recuriq.billing.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionRevenueDTO {
    private String region;       // "IN", "US", "GB"
    private long revenueMinor;   // MRR or ARR contribution in INR paise
}
