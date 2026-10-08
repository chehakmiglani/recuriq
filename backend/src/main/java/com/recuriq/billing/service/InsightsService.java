package com.recuriq.billing.service;

import java.util.List;

import com.recuriq.billing.dto.ai.InsightsDTO;
import com.recuriq.billing.dto.finance.MonthlyTrendDTO;

public interface InsightsService {

    /** Forecast, churn-risk highlights and (when AI is configured) a written briefing. */
    InsightsDTO getInsights(boolean refreshBriefing);

    /** Linear-trend MRR forecast for the given number of months after the last data point. */
    List<InsightsDTO.ForecastPoint> forecast(List<MonthlyTrendDTO> trend, int months);
}
