package com.recuriq.billing.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.service.ChurnRiskService;
import com.recuriq.billing.service.RevenueAnalyticsService;

import lombok.RequiredArgsConstructor;

/**
 * The read-only data tools the AI analyst may call, plus the prompts shared by every provider.
 *
 * The model never sees the database. It can only ask for one of these tools by name,
 * the backend runs it against the existing analytics services, and the JSON result
 * goes back to the model.
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    public static final String SYSTEM_PROMPT = """
            You are the RecurIQ billing analyst, built into the finance portal of a subscription \
            billing platform (demo brand: StreamFlix). You answer questions from finance and admin staff \
            about revenue, churn, invoices, payments and customer risk.

            Use the tools to get real figures before answering; never invent numbers. If the tools do not \
            have the data needed, say what is missing.

            Data conventions:
            - Fields ending in "Minor" are in minor units (paise): divide by 100 for rupees. Amounts are INR \
            unless a currency field says otherwise.
            - Fields named "amount" in invoice and payment records are already in rupees.
            - MRR = monthly recurring revenue, ARR = annual recurring revenue, ARPU = average revenue per \
            user, LTV = customer lifetime value.
            - Churn risk scores are 0-100 (HIGH >= 60, MEDIUM >= 30) and come with the reasons behind them.

            Answer style: lead with the direct answer, then the key figures and what they mean. Format \
            money as rupees with the ₹ sign and thousands separators. Keep it short and use a small \
            markdown table when comparing several items. When useful, end with one concrete recommendation.""";

    public static final String BRIEFING_PROMPT = """
            Write the finance team's briefing for the top of their dashboard from this snapshot. \
            Use 3 or 4 short sentences of plain prose (no headings, lists or tables): how revenue is \
            doing, where it is heading, the biggest risk, and one action to take this week.

            Snapshot:
            """;

    public static final int MAX_TOOL_ROUNDS = 8;
    private static final int MAX_HISTORY_TURNS = 10;

    /** One tool: name, description and JSON Schema for its arguments. */
    public record ToolSpec(String name, String description, Map<String, Object> properties, List<String> required) {

        public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", properties, "required", required);
        }
    }

    private static final Map<String, Object> LIMIT = Map.of("type", "integer", "minimum", 1, "maximum", 50,
            "description", "Maximum number of records to return, newest first (default 20).");

    public static final List<ToolSpec> SPECS = List.of(
            new ToolSpec("get_finance_dashboard",
                    "Headline KPIs: MRR, ARR, ARPU, LTV, net churn %, active customers, failed payment count, refund total, "
                            + "MRR by plan and by region, and the monthly MRR trend. Start here for broad questions.",
                    Map.of(), List.of()),
            new ToolSpec("get_mrr_report", "Current MRR and its month-by-month trend.", Map.of(), List.of()),
            new ToolSpec("get_arr_report", "Current ARR broken down by plan and by region.", Map.of(), List.of()),
            new ToolSpec("get_churn_report",
                    "Net churn %, revenue churn %, revenue lost to cancellations, monthly churn trend and cancellation reasons.",
                    Map.of(), List.of()),
            new ToolSpec("get_arpu_ltv_report", "Current ARPU and LTV with the monthly ARPU trend.", Map.of(), List.of()),
            new ToolSpec("get_churn_risk",
                    "Customers with live subscriptions scored 0-100 for how likely they are to cancel, with the reasons "
                            + "(failed payments, overdue invoices, pending cancellations, pauses, usage drops). "
                            + "Also returns counts per risk level and monthly revenue at risk.",
                    Map.of("level", Map.of("type", "string", "enum", List.of("HIGH", "MEDIUM", "LOW"),
                                    "description", "Only return customers at this risk level."),
                            "limit", LIMIT),
                    List.of()),
            new ToolSpec("get_recent_invoices", "Most recent invoices: number, customer id, amount, dates and status.",
                    Map.of("limit", LIMIT), List.of()),
            new ToolSpec("get_recent_payments",
                    "Most recent payment attempts: invoice, amount, payment method, date and status (SUCCESS, FAILED, REFUNDED...).",
                    Map.of("limit", LIMIT), List.of()),
            new ToolSpec("get_invoice_detail", "One invoice with its line items.",
                    Map.of("invoice_id", Map.of("type", "integer", "description", "Numeric invoice id.")),
                    List.of("invoice_id")),
            new ToolSpec("get_refunds", "Most recent refunds and credit notes with amounts, reasons and status.",
                    Map.of("limit", LIMIT), List.of()));

    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ChurnRiskService churnRiskService;
    private final ObjectMapper objectMapper;

    /** Runs a tool and returns its result as JSON. Throws if the tool or its arguments are invalid. */
    public String run(String name, Map<String, Object> input) throws JsonProcessingException {
        Map<String, Object> args = input == null ? Map.of() : input;
        Object data = switch (name) {
            case "get_finance_dashboard" -> revenueAnalyticsService.getFinanceDashboard();
            case "get_mrr_report" -> revenueAnalyticsService.getMrrReport();
            case "get_arr_report" -> revenueAnalyticsService.getArrReport();
            case "get_churn_report" -> revenueAnalyticsService.getChurnReport();
            case "get_arpu_ltv_report" -> revenueAnalyticsService.getArpuLtvReport();
            case "get_churn_risk" -> churnRisk(args);
            case "get_recent_invoices" -> revenueAnalyticsService
                    .getAllInvoiceRecords(PageRequest.of(0, limit(args))).getContent();
            case "get_recent_payments" -> revenueAnalyticsService
                    .getAllPaymentRecords(PageRequest.of(0, limit(args))).getContent();
            case "get_invoice_detail" -> revenueAnalyticsService.getInvoiceDetailById(invoiceId(args));
            case "get_refunds" -> revenueAnalyticsService
                    .getAllRefundCredits(PageRequest.of(0, limit(args))).getContent();
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
        return objectMapper.writeValueAsString(data);
    }

    /** Earlier chat turns, trimmed to the most recent and forced into strict user/assistant alternation. */
    public static List<AssistantRequest.ChatTurn> alternatingHistory(List<AssistantRequest.ChatTurn> history) {
        List<AssistantRequest.ChatTurn> result = new ArrayList<>();
        if (history == null) {
            return result;
        }
        String expected = "user";
        for (AssistantRequest.ChatTurn turn : history.subList(Math.max(0, history.size() - MAX_HISTORY_TURNS), history.size())) {
            if (turn.getContent() == null || turn.getContent().isBlank() || !expected.equals(turn.getRole())) {
                continue;
            }
            result.add(turn);
            expected = "user".equals(expected) ? "assistant" : "user";
        }
        // History ended on an unanswered question; close it so the new question keeps the alternation
        if ("assistant".equals(expected)) {
            AssistantRequest.ChatTurn placeholder = new AssistantRequest.ChatTurn();
            placeholder.setRole("assistant");
            placeholder.setContent("(no answer)");
            result.add(placeholder);
        }
        return result;
    }

    private ChurnRiskSummaryDTO churnRisk(Map<String, Object> input) {
        ChurnRiskSummaryDTO summary = churnRiskService.getChurnRisk((String) input.get("level"));
        int limit = limit(input);
        if (summary.getCustomers().size() > limit) {
            summary.setCustomers(summary.getCustomers().subList(0, limit));
        }
        return summary;
    }

    private static long invoiceId(Map<String, Object> input) {
        Object value = input.get("invoice_id");
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && s.matches("\\d+")) {
            return Long.parseLong(s);
        }
        throw new IllegalArgumentException("invoice_id must be a number");
    }

    private static int limit(Map<String, Object> input) {
        Object value = input.get("limit");
        int limit = value instanceof Number n ? n.intValue()
                : value instanceof String s && s.matches("\\d+") ? Integer.parseInt(s) : 20;
        return Math.max(1, Math.min(limit, 50));
    }
}
