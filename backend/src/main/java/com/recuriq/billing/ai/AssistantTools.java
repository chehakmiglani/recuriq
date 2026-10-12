package com.recuriq.billing.ai;

import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            - Every amount in the tool results is already in rupees (INR) unless a currency field says \
            otherwise. Copy figures exactly as given; do not rescale or round them.
            - An invoice's total is its "amount" field. Tax can already be included in prices, so never \
            work out a total by adding up line items.
            - MRR = monthly recurring revenue, ARR = annual recurring revenue, ARPU = average revenue per \
            user, LTV = customer lifetime value. Always say whether a revenue figure is per month or per \
            year. The dashboard's plan and region breakdowns are monthly; the ARR report's are annual.
            - Churn risk scores are 0-100 (HIGH >= 60, MEDIUM >= 30) and come with the reasons behind them.
            - Trend lists (revenueTrend, arrTrend, arpuTrend, churnTrend) are stored month-end snapshots. \
            They can be on a different basis from the live KPI figures, so never work out growth by \
            comparing a live figure with a snapshot; compare snapshots only with other snapshots. Ignore \
            snapshot months later than today's date, and never present a snapshot as a projection.

            Answer style: lead with the direct answer, then the key figures and what they mean. Write \
            money in full rupees with the ₹ sign and thousands separators (₹14,453.60, not "14.5k" or \
            "thousands"). Keep the whole answer under about 150 words unless the user asks for detail, \
            with at most one small markdown table and only the figures that answer the question. When \
            useful, end with one concrete recommendation in a single sentence.""";

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
            new ToolSpec("get_arr_report", "Current ARR broken down by plan and by region. All figures are annual (12 x monthly).",
                    Map.of(), List.of()),
            new ToolSpec("get_churn_report",
                    "Net churn %, revenue churn %, revenue lost to cancellations, monthly churn trend and cancellation reasons.",
                    Map.of(), List.of()),
            new ToolSpec("get_arpu_ltv_report", "Current ARPU and LTV with the monthly ARPU trend.", Map.of(), List.of()),
            new ToolSpec("get_churn_risk",
                    "Customers with live subscriptions scored 0-100 for how likely they are to cancel, with the reasons "
                            + "(failed payments, overdue invoices, pending cancellations, pauses, usage drops). "
                            + "Also returns counts per risk level and monthly revenue at risk. Results are sorted by score, "
                            + "highest first; leave out level to get the riskiest customers whatever their level.",
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
        return objectMapper.writeValueAsString(forModel(objectMapper.valueToTree(data)));
    }

    /** Prepares service data for a language model: amounts in rupees and trends clearly labelled. */
    public static JsonNode forModel(JsonNode node) {
        return toRupees(labelTrends(node));
    }

    /**
     * Replaces every "...Minor" amount (paise) with the same field in rupees, e.g. mrrMinor 1445360
     * becomes mrr 14453.60. Asking the model to divide by 100 itself produced wrong digits.
     */
    private static JsonNode toRupees(JsonNode node) {
        if (node instanceof ArrayNode array) {
            array.forEach(AssistantTools::toRupees);
        } else if (node instanceof ObjectNode object) {
            List<String> fields = new ArrayList<>();
            object.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                JsonNode value = object.get(field);
                if (field.endsWith("Minor") && value.isNumber()) {
                    object.remove(field);
                    object.put(field.substring(0, field.length() - "Minor".length()),
                            value.decimalValue().movePointLeft(2).setScale(2, RoundingMode.HALF_UP));
                } else {
                    toRupees(value);
                }
            }
        }
        return node;
    }

    /**
     * Makes trend lists safe for the model to read: drops snapshot months that are still in the
     * future and says outright that snapshots are not comparable with the live figures. Models
     * otherwise tend to compute "growth" between a live KPI and an old snapshot.
     */
    private static JsonNode labelTrends(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            return node;
        }
        String currentMonth = YearMonth.now().toString();
        boolean hasTrend = false;
        List<String> fields = new ArrayList<>();
        object.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) {
            if (!field.endsWith("Trend") || !object.get(field).isArray()) {
                continue;
            }
            hasTrend = true;
            ArrayNode kept = object.arrayNode();
            for (JsonNode point : object.get(field)) {
                if (point.path("month").asText("").compareTo(currentMonth) <= 0) {
                    kept.add(point);
                }
            }
            object.set(field, kept);
        }
        if (hasTrend) {
            object.put("trendNote", "Trend lists are month-end snapshots stored earlier. They are not on the same basis as the "
                    + "live figures in this response, so do not compare a trend value with a live value.");
        }
        return object;
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
