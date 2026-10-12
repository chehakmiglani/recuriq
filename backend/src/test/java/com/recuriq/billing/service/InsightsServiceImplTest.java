package com.recuriq.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.ChurnRiskDTO;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.dto.ai.InsightsDTO;
import com.recuriq.billing.dto.finance.FinanceDashboardDTO;
import com.recuriq.billing.dto.finance.MonthlyTrendDTO;

@ExtendWith(MockitoExtension.class)
class InsightsServiceImplTest {

    @Mock
    private RevenueAnalyticsService revenueAnalyticsService;
    @Mock
    private ChurnRiskService churnRiskService;
    @Mock
    private AiAssistantService aiAssistantService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InsightsServiceImpl insightsService;

    @Test
    void forecast_extendsLinearTrend() {
        List<InsightsDTO.ForecastPoint> points = insightsService.forecast(
                List.of(trend("2026-07", 1000), trend("2026-08", 1100), trend("2026-09", 1200)), 3);

        assertEquals(3, points.size());
        assertEquals("2026-10", points.get(0).getMonth());
        assertEquals(1300, points.get(0).getValueMinor());
        assertEquals("2026-12", points.get(2).getMonth());
        assertEquals(1500, points.get(2).getValueMinor());
    }

    @Test
    void forecast_crossesYearBoundary() {
        List<InsightsDTO.ForecastPoint> points = insightsService.forecast(
                List.of(trend("2026-11", 500), trend("2026-12", 500)), 2);

        assertEquals("2027-01", points.get(0).getMonth());
        assertEquals("2027-02", points.get(1).getMonth());
        assertEquals(500, points.get(1).getValueMinor());
    }

    @Test
    void forecast_neverGoesNegative() {
        List<InsightsDTO.ForecastPoint> points = insightsService.forecast(
                List.of(trend("2026-07", 300), trend("2026-08", 100)), 3);

        assertTrue(points.stream().allMatch(p -> p.getValueMinor() >= 0));
        assertEquals(0, points.get(2).getValueMinor());
    }

    @Test
    void forecast_needsAtLeastTwoMonths() {
        assertTrue(insightsService.forecast(List.of(trend("2026-07", 300)), 3).isEmpty());
        assertTrue(insightsService.forecast(null, 3).isEmpty());
    }

    @Test
    void getInsights_withoutAi_returnsForecastAndTopRisks() {
        FinanceDashboardDTO dashboard = FinanceDashboardDTO.builder()
                .mrrMinor(1200)
                .revenueTrend(List.of(trend("2026-08", 1000), trend("2026-09", 1200)))
                .build();
        ChurnRiskDTO risky = ChurnRiskDTO.builder().customerId(1L).score(70).level("HIGH").build();
        ChurnRiskDTO safe = ChurnRiskDTO.builder().customerId(2L).score(0).level("LOW").build();
        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(dashboard);
        when(churnRiskService.getChurnRisk(null)).thenReturn(ChurnRiskSummaryDTO.builder()
                .highRisk(1).monthlyRevenueAtRiskMinor(49900).customers(List.of(risky, safe)).build());
        when(aiAssistantService.isEnabled()).thenReturn(false);

        InsightsDTO insights = insightsService.getInsights(false);

        assertEquals(3, insights.getMrrForecast().size());
        assertEquals(50.0, insights.getProjectedGrowthPercent());   // last month 1200 -> 1800
        assertEquals(List.of(risky), insights.getTopRisks());
        assertNull(insights.getAiBriefing());
        verify(aiAssistantService, never()).writeBriefing(anyString());
    }

    @Test
    void getInsights_withAi_sendsRupeeFiguresAndNoRawTrend() throws Exception {
        FinanceDashboardDTO dashboard = FinanceDashboardDTO.builder()
                .mrrMinor(1445360).arrMinor(17344320).activeCustomers(27)
                .revenueTrend(List.of(trend("2026-08", 1000), trend("2026-09", 1100)))
                .build();
        when(revenueAnalyticsService.getFinanceDashboard()).thenReturn(dashboard);
        when(churnRiskService.getChurnRisk(null)).thenReturn(ChurnRiskSummaryDTO.builder()
                .mediumRisk(3).monthlyRevenueAtRiskMinor(49900).customers(List.of()).build());
        when(aiAssistantService.isEnabled()).thenReturn(true);
        when(aiAssistantService.writeBriefing(anyString())).thenReturn("Revenue is steady.");

        InsightsDTO insights = insightsService.getInsights(false);
        insightsService.getInsights(false);   // second call should reuse the cached briefing

        assertEquals("Revenue is steady.", insights.getAiBriefing());
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(aiAssistantService, times(1)).writeBriefing(sent.capture());
        assertTrue(sent.getValue().contains("\"mrr\":14453.60"), sent.getValue());
        assertTrue(sent.getValue().contains("\"arr\":173443.20"), sent.getValue());
        assertTrue(sent.getValue().contains("\"monthlyRevenueAtRisk\":499.00"), sent.getValue());
        assertFalse(sent.getValue().contains("Minor"), "amounts must already be in rupees");
        assertFalse(sent.getValue().contains("revenueTrend"), "raw snapshot series must not be sent");
    }

    @Test
    void monthly_keepsLastSnapshotPerMonthInOrder() {
        List<MonthlyTrendDTO> result = InsightsServiceImpl.monthly(List.of(
                trend("2026-06", 600), trend("2026-05", 9999), trend("2026-05", 500)));

        assertEquals(List.of("2026-05", "2026-06"), result.stream().map(MonthlyTrendDTO::getMonth).toList());
        assertEquals(500, result.get(0).getValueMinor());
    }

    private static MonthlyTrendDTO trend(String month, long value) {
        MonthlyTrendDTO dto = new MonthlyTrendDTO();
        dto.setMonth(month);
        dto.setValueMinor(value);
        return dto;
    }
}
