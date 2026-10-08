package com.recuriq.billing.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * AI billing assistant backed by Claude.
 *
 * Claude never sees the database directly. It is given a small set of read-only
 * tools that call the existing analytics services, decides which ones it needs
 * for the question, and writes its answer from the numbers those tools return.
 */
@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final int MAX_TOOL_ROUNDS = 8;
    private static final int MAX_HISTORY_TURNS = 10;

    private static final String SYSTEM_PROMPT = """
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

    private final Optional<AnthropicClient> client;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ChurnRiskService churnRiskService;
    private final ObjectMapper objectMapper;
    private final String model;
    private final List<Tool> tools = buildTools();

    public AiAssistantServiceImpl(Optional<AnthropicClient> client,
                                  RevenueAnalyticsService revenueAnalyticsService,
                                  ChurnRiskService churnRiskService,
                                  ObjectMapper objectMapper,
                                  @Value("${recuriq.ai.model:claude-opus-5-5}") String model) {
        this.client = client;
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.churnRiskService = churnRiskService;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public boolean isEnabled() {
        return client.isPresent();
    }

    @Override
    public AssistantResponse ask(AssistantRequest request) {
        AnthropicClient anthropic = client.orElseThrow(() -> new CustomException(
                "The AI assistant is not configured. Set the ANTHROPIC_API_KEY environment variable and restart the backend.",
                HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED"));

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT + "\n\nToday's date: " + LocalDate.now())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
                // If a safety classifier declines the request, let the API retry it on a fallback model
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        tools.forEach(params::addTool);
        addHistory(params, request.getHistory());
        params.addUserMessage(request.getQuestion());

        List<String> toolsUsed = new ArrayList<>();
        try {
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                Message response = anthropic.messages().create(params.build());
                StopReason stop = response.stopReason().orElse(StopReason.END_TURN);

                if (stop.equals(StopReason.REFUSAL)) {
                    return new AssistantResponse("I can't help with that request. Try asking about revenue, churn, invoices or payments.", toolsUsed);
                }
                if (!stop.equals(StopReason.TOOL_USE)) {
                    return new AssistantResponse(extractText(response), toolsUsed);
                }

                // Keep the assistant turn (including its thinking) exactly as returned, then answer every tool call in one message
                params.addMessage(response);
                List<ContentBlockParam> results = new ArrayList<>();
                for (var block : response.content()) {
                    block.toolUse().ifPresent(call -> {
                        toolsUsed.add(call.name());
                        results.add(ContentBlockParam.ofToolResult(runTool(call)));
                    });
                }
                params.addMessage(MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(results)
                        .build());
            }
            return new AssistantResponse("That question needed more steps than I'm allowed to take. Try asking something more specific.", toolsUsed);
        } catch (UnauthorizedException e) {
            log.error("Anthropic API key was rejected", e);
            throw new CustomException("The AI assistant's API key is invalid.", HttpStatus.SERVICE_UNAVAILABLE, "AI_AUTH_FAILED");
        } catch (RateLimitException e) {
            throw new CustomException("The AI assistant is busy. Please try again in a minute.", HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED");
        } catch (AnthropicServiceException e) {
            log.error("Anthropic API error {}", e.statusCode(), e);
            throw new CustomException("The AI service returned an error. Please try again.", HttpStatus.BAD_GATEWAY, "AI_UPSTREAM_ERROR");
        } catch (AnthropicException e) {
            log.error("Could not reach the Anthropic API", e);
            throw new CustomException("Could not reach the AI service. Check the server's internet connection.", HttpStatus.BAD_GATEWAY, "AI_UNREACHABLE");
        }
    }

    @Override
    public String writeBriefing(String snapshotJson) {
        AnthropicClient anthropic = client.orElseThrow(() -> new CustomException(
                "The AI assistant is not configured.", HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED"));
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4000L)
                .system(SYSTEM_PROMPT + "\n\nToday's date: " + LocalDate.now())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessage("""
                        Write the finance team's briefing for the top of their dashboard from this snapshot. \
                        Use 3 or 4 short sentences of plain prose (no headings, lists or tables): how revenue is \
                        doing, where it is heading, the biggest risk, and one action to take this week.

                        Snapshot:
                        """ + snapshotJson)
                .build();
        try {
            Message response = anthropic.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
            return text.isEmpty() ? null : text.toString().trim();
        } catch (AnthropicException e) {
            log.warn("Could not generate the AI briefing: {}", e.getMessage());
            return null;
        }
    }

    // ─── Tools ───────────────────────────────────────────────────────────────

    private ToolResultBlockParam runTool(ToolUseBlock call) {
        ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(call.id());
        try {
            Map<String, Object> input = call._input().convert(Map.class);
            Object data = switch (call.name()) {
                case "get_finance_dashboard" -> revenueAnalyticsService.getFinanceDashboard();
                case "get_mrr_report" -> revenueAnalyticsService.getMrrReport();
                case "get_arr_report" -> revenueAnalyticsService.getArrReport();
                case "get_churn_report" -> revenueAnalyticsService.getChurnReport();
                case "get_arpu_ltv_report" -> revenueAnalyticsService.getArpuLtvReport();
                case "get_churn_risk" -> churnRisk(input);
                case "get_recent_invoices" -> revenueAnalyticsService
                        .getAllInvoiceRecords(PageRequest.of(0, limit(input))).getContent();
                case "get_recent_payments" -> revenueAnalyticsService
                        .getAllPaymentRecords(PageRequest.of(0, limit(input))).getContent();
                case "get_invoice_detail" -> revenueAnalyticsService
                        .getInvoiceDetailById(((Number) input.get("invoice_id")).longValue());
                case "get_refunds" -> revenueAnalyticsService
                        .getAllRefundCredits(PageRequest.of(0, limit(input))).getContent();
                default -> throw new IllegalArgumentException("Unknown tool: " + call.name());
            };
            return result.content(objectMapper.writeValueAsString(data)).build();
        } catch (Exception e) {
            log.warn("Assistant tool {} failed: {}", call.name(), e.getMessage());
            return result.content("Error: " + e.getMessage()).isError(true).build();
        }
    }

    private ChurnRiskSummaryDTO churnRisk(Map<String, Object> input) {
        ChurnRiskSummaryDTO summary = churnRiskService.getChurnRisk((String) input.get("level"));
        int limit = limit(input);
        if (summary.getCustomers().size() > limit) {
            summary.setCustomers(summary.getCustomers().subList(0, limit));
        }
        return summary;
    }

    private static int limit(Map<String, Object> input) {
        Object value = input.get("limit");
        int limit = value instanceof Number n ? n.intValue() : 20;
        return Math.max(1, Math.min(limit, 50));
    }

    private static List<Tool> buildTools() {
        Map<String, Object> limit = Map.of("type", "integer", "minimum", 1, "maximum", 50,
                "description", "Maximum number of records to return, newest first (default 20).");
        return List.of(
                tool("get_finance_dashboard",
                        "Headline KPIs: MRR, ARR, ARPU, LTV, net churn %, active customers, failed payment count, refund total, "
                                + "MRR by plan and by region, and the monthly MRR trend. Start here for broad questions.",
                        Map.of()),
                tool("get_mrr_report", "Current MRR and its month-by-month trend.", Map.of()),
                tool("get_arr_report", "Current ARR broken down by plan and by region.", Map.of()),
                tool("get_churn_report",
                        "Net churn %, revenue churn %, revenue lost to cancellations, monthly churn trend and cancellation reasons.",
                        Map.of()),
                tool("get_arpu_ltv_report", "Current ARPU and LTV with the monthly ARPU trend.", Map.of()),
                tool("get_churn_risk",
                        "Customers with live subscriptions scored 0-100 for how likely they are to cancel, with the reasons "
                                + "(failed payments, overdue invoices, pending cancellations, pauses, usage drops). "
                                + "Also returns counts per risk level and monthly revenue at risk.",
                        Map.of("level", Map.of("type", "string", "enum", List.of("HIGH", "MEDIUM", "LOW"),
                                        "description", "Only return customers at this risk level."),
                                "limit", limit)),
                tool("get_recent_invoices", "Most recent invoices: number, customer id, amount, dates and status.",
                        Map.of("limit", limit)),
                tool("get_recent_payments",
                        "Most recent payment attempts: invoice, amount, payment method, date and status (SUCCESS, FAILED, REFUNDED...).",
                        Map.of("limit", limit)),
                tool("get_invoice_detail", "One invoice with its line items.",
                        Map.of("invoice_id", Map.of("type", "integer", "description", "Numeric invoice id.")),
                        "invoice_id"),
                tool("get_refunds", "Most recent refunds and credit notes with amounts, reasons and status.",
                        Map.of("limit", limit)));
    }

    private static Tool tool(String name, String description, Map<String, Object> properties, String... required) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        properties.forEach((key, schema) -> props.putAdditionalProperty(key, JsonValue.from(schema)));
        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(props.build())
                        .required(List.of(required))
                        .build())
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Replays earlier chat turns as plain text, keeping strict user/assistant alternation. */
    private static void addHistory(MessageCreateParams.Builder params, List<AssistantRequest.ChatTurn> history) {
        if (history == null) {
            return;
        }
        List<AssistantRequest.ChatTurn> recent = history.subList(Math.max(0, history.size() - MAX_HISTORY_TURNS), history.size());
        String expected = "user";
        for (AssistantRequest.ChatTurn turn : recent) {
            if (turn.getContent() == null || turn.getContent().isBlank() || !expected.equals(turn.getRole())) {
                continue;
            }
            if ("user".equals(expected)) {
                params.addUserMessage(turn.getContent());
                expected = "assistant";
            } else {
                params.addAssistantMessage(turn.getContent());
                expected = "user";
            }
        }
        // History ended on an unanswered question; close it so the new question keeps the alternation
        if ("assistant".equals(expected)) {
            params.addAssistantMessage("(no answer)");
        }
    }

    private static String extractText(Message response) {
        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        return text.isEmpty() ? "I couldn't produce an answer. Please rephrase the question." : text.toString().trim();
    }
}
