package com.recuriq.billing.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.ChurnRiskDTO;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.service.ChurnRiskService;
import com.recuriq.billing.service.RevenueAnalyticsService;

@ExtendWith(MockitoExtension.class)
class AssistantToolsTest {

    @Mock
    private RevenueAnalyticsService revenueAnalyticsService;
    @Mock
    private ChurnRiskService churnRiskService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AssistantTools tools;

    @Test
    void everyToolHasAnObjectSchema() {
        assertEquals(10, AssistantTools.SPECS.size());
        AssistantTools.SPECS.forEach(spec -> assertEquals("object", spec.inputSchema().get("type"), spec.name()));
    }

    @Test
    void churnRisk_appliesLevelAndLimit() throws Exception {
        List<ChurnRiskDTO> customers = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            customers.add(ChurnRiskDTO.builder().customerId(i).build());
        }
        when(churnRiskService.getChurnRisk("HIGH")).thenReturn(ChurnRiskSummaryDTO.builder().customers(customers).build());

        String json = tools.run("get_churn_risk", Map.of("level", "HIGH", "limit", 2));

        assertEquals(2, objectMapper.readTree(json).path("customers").size());
    }

    @Test
    void limit_isClampedAndAcceptsStrings() throws Exception {
        when(revenueAnalyticsService.getAllPaymentRecords(any())).thenReturn(new PageImpl<>(List.of()));

        tools.run("get_recent_payments", Map.of("limit", 500));
        verify(revenueAnalyticsService).getAllPaymentRecords(PageRequest.of(0, 50));

        tools.run("get_recent_payments", Map.of("limit", "7"));
        verify(revenueAnalyticsService).getAllPaymentRecords(PageRequest.of(0, 7));
    }

    @Test
    void invoiceDetail_requiresNumericId() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> tools.run("get_invoice_detail", Map.of("invoice_id", "abc")));
        tools.run("get_invoice_detail", Map.of("invoice_id", "12"));
        verify(revenueAnalyticsService).getInvoiceDetailById(12L);
    }

    @Test
    void unknownTool_throws() {
        assertThrows(IllegalArgumentException.class, () -> tools.run("drop_tables", Map.of()));
    }

    @Test
    void alternatingHistory_dropsOutOfOrderTurnsAndClosesOpenQuestion() {
        List<AssistantRequest.ChatTurn> result = AssistantTools.alternatingHistory(List.of(
                turn("assistant", "hello"),      // can't start with assistant
                turn("user", "q1"),
                turn("user", "q1 again"),        // two users in a row
                turn("assistant", "a1"),
                turn("user", "  "),              // blank
                turn("user", "q2")));            // unanswered

        assertEquals(List.of("user:q1", "assistant:a1", "user:q2", "assistant:(no answer)"),
                result.stream().map(t -> t.getRole() + ":" + t.getContent()).toList());
        assertTrue(AssistantTools.alternatingHistory(null).isEmpty());
    }

    private static AssistantRequest.ChatTurn turn(String role, String content) {
        AssistantRequest.ChatTurn t = new AssistantRequest.ChatTurn();
        t.setRole(role);
        t.setContent(content);
        return t;
    }
}
