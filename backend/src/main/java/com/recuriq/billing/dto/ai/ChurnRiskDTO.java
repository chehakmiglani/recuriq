package com.recuriq.billing.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnRiskDTO {
    private Long customerId;
    private String customerName;
    private String email;
    private String country;
    private String planName;
    private String subscriptionStatus;
    private long monthlyValueMinor;   // plan price normalised to one month, minor units
    private String currency;
    private int score;                // 0-100, higher = more likely to churn
    private String level;             // HIGH / MEDIUM / LOW
    private List<String> reasons;     // human-readable signals behind the score
}
