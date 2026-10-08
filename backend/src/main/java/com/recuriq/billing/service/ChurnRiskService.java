package com.recuriq.billing.service;

import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;

public interface ChurnRiskService {

    /** Scores every customer with a live subscription; level filter is optional (HIGH / MEDIUM / LOW). */
    ChurnRiskSummaryDTO getChurnRisk(String level);
}
