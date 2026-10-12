package com.recuriq.billing.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.ai.AssistantTools;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.dto.ai.InsightsDTO;
import com.recuriq.billing.dto.finance.FinanceDashboardDTO;
import com.recuriq.billing.dto.finance.MonthlyTrendDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightsServiceImpl implements InsightsService {

    static final int FORECAST_MONTHS = 3;
    private static final int TREND_WINDOW = 6;
    private static final Duration BRIEFING_TTL = Duration.ofMinutes(30);

    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ChurnRiskService churnRiskService;
    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;

    // The briefing costs an API call, so it is reused for everyone until it expires
    private String cachedBriefing;
    private LocalDateTime briefingGeneratedAt;

    @Override
    public InsightsDTO getInsights(boolean refreshBriefing) {
        FinanceDashboardDTO dashboard = revenueAnalyticsService.getFinanceDashboard();
        ChurnRiskSummaryDTO risk = churnRiskService.getChurnRisk(null);

        List<MonthlyTrendDTO> trend = monthly(dashboard.getRevenueTrend());
        List<InsightsDTO.ForecastPoint> forecast = forecast(trend, FORECAST_MONTHS);
        Double growth = null;
        if (!forecast.isEmpty()) {
            // Compare against the trend's own latest month so both numbers come from snapshots
            long base = trend.get(trend.size() - 1).getValueMinor();
            long last = forecast.get(forecast.size() - 1).getValueMinor();
            growth = base > 0 ? Math.round(1000.0 * (last - base) / base) / 10.0 : null;
        }

        InsightsDTO insights = InsightsDTO.builder()
                .mrrForecast(forecast)
                .currentMrrMinor(dashboard.getMrrMinor())
                .projectedGrowthPercent(growth)
                .highRisk(risk.getHighRisk())
                .mediumRisk(risk.getMediumRisk())
                .monthlyRevenueAtRiskMinor(risk.getMonthlyRevenueAtRiskMinor())
                .topRisks(risk.getCustomers().stream().filter(r -> r.getScore() > 0).limit(3).toList())
                .build();

        if (aiAssistantService.isEnabled()) {
            addBriefing(insights, dashboard, refreshBriefing);
        }
        return insights;
    }

    private synchronized void addBriefing(InsightsDTO insights, FinanceDashboardDTO dashboard, boolean refresh) {
        boolean fresh = cachedBriefing != null && briefingGeneratedAt != null
                && briefingGeneratedAt.plus(BRIEFING_TTL).isAfter(LocalDateTime.now());
        if (refresh || !fresh) {
            try {
                String briefing = aiAssistantService.writeBriefing(objectMapper.writeValueAsString(
                        AssistantTools.forModel(objectMapper.valueToTree(briefingSnapshot(insights, dashboard)))));
                if (briefing != null) {
                    cachedBriefing = briefing;
                    briefingGeneratedAt = LocalDateTime.now();
                }
            } catch (JsonProcessingException e) {
                log.warn("Could not serialise the insights snapshot", e);
            }
        }
        insights.setAiBriefing(cachedBriefing);
        insights.setAiBriefingGeneratedAt(briefingGeneratedAt);
    }

    /**
     * The figures the briefing is written from. The projection is the trend's growth rate applied
     * to live MRR, as on the dashboard card; the raw snapshot series is left out because it is on
     * a different basis from the live figures and models mix the two up.
     */
    private static Map<String, Object> briefingSnapshot(InsightsDTO insights, FinanceDashboardDTO dashboard) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mrrMinor", dashboard.getMrrMinor());
        snapshot.put("arrMinor", dashboard.getArrMinor());
        snapshot.put("arpuMinor", dashboard.getArpuMinor());
        snapshot.put("ltvMinor", dashboard.getLtvMinor());
        snapshot.put("netChurnPercent", dashboard.getNetChurnPercent());
        snapshot.put("activeCustomers", dashboard.getActiveCustomers());
        snapshot.put("failedPaymentsCount", dashboard.getFailedPaymentsCount());
        snapshot.put("refundAmountMinor", dashboard.getRefundAmountMinor());
        snapshot.put("mrrByPlan", dashboard.getRevenueByPlan());
        snapshot.put("mrrByRegion", dashboard.getRevenueByRegion());
        Double growth = insights.getProjectedGrowthPercent();
        if (growth != null) {
            snapshot.put("projectedMrrGrowthPercentOver3Months", growth);
            snapshot.put("projectedMrrIn3MonthsMinor", Math.round(dashboard.getMrrMinor() * (1 + growth / 100)));
        }
        Map<String, Object> churnRisk = new LinkedHashMap<>();
        churnRisk.put("highRiskCustomers", insights.getHighRisk());
        churnRisk.put("mediumRiskCustomers", insights.getMediumRisk());
        churnRisk.put("monthlyRevenueAtRiskMinor", insights.getMonthlyRevenueAtRiskMinor());
        churnRisk.put("mostAtRisk", insights.getTopRisks());
        snapshot.put("churnRisk", churnRisk);
        return snapshot;
    }

    /** One point per month (the last snapshot recorded for it), oldest first. */
    static List<MonthlyTrendDTO> monthly(List<MonthlyTrendDTO> trend) {
        if (trend == null) {
            return List.of();
        }
        Map<String, MonthlyTrendDTO> byMonth = new TreeMap<>();
        trend.forEach(t -> byMonth.put(t.getMonth(), t));
        return new ArrayList<>(byMonth.values());
    }

    /** Least-squares line through the last few months of MRR, extended forward. */
    @Override
    public List<InsightsDTO.ForecastPoint> forecast(List<MonthlyTrendDTO> trend, int months) {
        if (trend == null || trend.size() < 2) {
            return List.of();
        }
        List<MonthlyTrendDTO> window = trend.subList(Math.max(0, trend.size() - TREND_WINDOW), trend.size());
        int n = window.size();
        double meanX = (n - 1) / 2.0;
        double meanY = window.stream().mapToLong(MonthlyTrendDTO::getValueMinor).average().orElse(0);
        double num = 0;
        double den = 0;
        for (int i = 0; i < n; i++) {
            num += (i - meanX) * (window.get(i).getValueMinor() - meanY);
            den += (i - meanX) * (i - meanX);
        }
        double slope = num / den;
        double intercept = meanY - slope * meanX;

        YearMonth lastMonth = YearMonth.parse(window.get(n - 1).getMonth());
        List<InsightsDTO.ForecastPoint> points = new ArrayList<>();
        for (int k = 1; k <= months; k++) {
            long value = Math.max(0, Math.round(intercept + slope * (n - 1 + k)));
            points.add(new InsightsDTO.ForecastPoint(lastMonth.plusMonths(k).toString(), value));
        }
        return points;
    }
}
