package com.recuriq.billing.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;

import lombok.extern.slf4j.Slf4j;

/** Anthropic Claude via the official Java SDK, using tool use. */
@Slf4j
public class ClaudeProvider implements LlmProvider {

    private final AnthropicClient client;
    private final AssistantTools tools;
    private final String model;
    private final List<Tool> toolDefinitions;

    public ClaudeProvider(AnthropicClient client, AssistantTools tools, String model) {
        this.client = client;
        this.tools = tools;
        this.model = model;
        this.toolDefinitions = AssistantTools.SPECS.stream().map(ClaudeProvider::toTool).toList();
    }

    @Override
    public String displayName() {
        return "Claude";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public AssistantResponse ask(List<AssistantRequest.ChatTurn> history, String question) {
        MessageCreateParams.Builder params = baseParams(OutputConfig.Effort.MEDIUM).maxTokens(16000L);
        toolDefinitions.forEach(params::addTool);
        for (AssistantRequest.ChatTurn turn : history) {
            if ("user".equals(turn.getRole())) {
                params.addUserMessage(turn.getContent());
            } else {
                params.addAssistantMessage(turn.getContent());
            }
        }
        params.addUserMessage(question);

        List<String> toolsUsed = new ArrayList<>();
        try {
            for (int round = 0; round < AssistantTools.MAX_TOOL_ROUNDS; round++) {
                Message response = client.messages().create(params.build());
                StopReason stop = response.stopReason().orElse(StopReason.END_TURN);

                if (stop.equals(StopReason.REFUSAL)) {
                    return new AssistantResponse("I can't help with that request. Try asking about revenue, churn, invoices or payments.", toolsUsed);
                }
                if (!stop.equals(StopReason.TOOL_USE)) {
                    String text = text(response);
                    return new AssistantResponse(text.isEmpty() ? "I couldn't produce an answer. Please rephrase the question." : text, toolsUsed);
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
            throw AiErrors.badKey();
        } catch (RateLimitException e) {
            throw AiErrors.rateLimited();
        } catch (AnthropicServiceException e) {
            log.error("Anthropic API error {}", e.statusCode(), e);
            throw AiErrors.upstream();
        } catch (AnthropicException e) {
            log.error("Could not reach the Anthropic API", e);
            throw AiErrors.unreachable();
        }
    }

    @Override
    public String complete(String prompt) {
        MessageCreateParams params = baseParams(OutputConfig.Effort.LOW)
                .maxTokens(4000L)
                .addUserMessage(prompt)
                .build();
        try {
            Message response = client.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                return null;
            }
            String text = text(response);
            return text.isEmpty() ? null : text;
        } catch (AnthropicException e) {
            log.warn("Claude completion failed: {}", e.getMessage());
            return null;
        }
    }

    private MessageCreateParams.Builder baseParams(OutputConfig.Effort effort) {
        return MessageCreateParams.builder()
                .model(model)
                .system(AssistantTools.SYSTEM_PROMPT + "\n\nToday's date: " + LocalDate.now())
                .outputConfig(OutputConfig.builder().effort(effort).build())
                // If a safety classifier declines the request, let the API retry it on a fallback model
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
    }

    @SuppressWarnings("unchecked")
    private ToolResultBlockParam runTool(ToolUseBlock call) {
        ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(call.id());
        try {
            return result.content(tools.run(call.name(), call._input().convert(Map.class))).build();
        } catch (Exception e) {
            log.warn("Assistant tool {} failed: {}", call.name(), e.getMessage());
            return result.content("Error: " + e.getMessage()).isError(true).build();
        }
    }

    private static Tool toTool(AssistantTools.ToolSpec spec) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        spec.properties().forEach((key, schema) -> props.putAdditionalProperty(key, JsonValue.from(schema)));
        return Tool.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(Tool.InputSchema.builder()
                        .properties(props.build())
                        .required(spec.required())
                        .build())
                .build();
    }

    private static String text(Message response) {
        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        return text.toString().trim();
    }
}
