package com.recuriq.billing.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Groq (free tier available) through its OpenAI-compatible chat completions API,
 * using function calling for the analyst's tools.
 */
@Slf4j
public class GroqProvider implements LlmProvider {

    public static final String BASE_URL = "https://api.groq.com/openai/v1";
    static final long MAX_RATE_LIMIT_WAIT_SECONDS = 20;

    private final RestClient http;
    private final AssistantTools tools;
    private final ObjectMapper objectMapper;
    private final String model;
    private final List<Map<String, Object>> toolDefinitions;

    public GroqProvider(RestClient.Builder builder, String apiKey, AssistantTools tools,
                        ObjectMapper objectMapper, String model) {
        this.http = builder
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.tools = tools;
        this.objectMapper = objectMapper;
        this.model = model;
        this.toolDefinitions = AssistantTools.SPECS.stream()
                .map(spec -> Map.<String, Object>of("type", "function", "function", Map.of(
                        "name", spec.name(),
                        "description", spec.description(),
                        "parameters", spec.inputSchema())))
                .toList();
    }

    @Override
    public String displayName() {
        return "Groq";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public AssistantResponse ask(List<AssistantRequest.ChatTurn> history, String question) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", AssistantTools.SYSTEM_PROMPT + "\n\nToday's date: " + LocalDate.now()));
        history.forEach(turn -> messages.add(message(turn.getRole(), turn.getContent())));
        messages.add(message("user", question));

        List<String> toolsUsed = new ArrayList<>();
        for (int round = 0; round < AssistantTools.MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("tools", toolDefinitions);
            body.put("tool_choice", "auto");
            body.put("temperature", 0.2);
            body.put("max_completion_tokens", 2000);

            JsonNode reply = send(body).path("choices").path(0).path("message");
            JsonNode calls = reply.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                String text = reply.path("content").asText("").trim();
                return new AssistantResponse(text.isEmpty() ? "I couldn't produce an answer. Please rephrase the question." : text, toolsUsed);
            }

            // Echo the assistant's tool calls back, then add one tool message per call
            Map<String, Object> assistant = new LinkedHashMap<>();
            assistant.put("role", "assistant");
            assistant.put("content", reply.path("content").isTextual() ? reply.path("content").asText() : "");
            assistant.put("tool_calls", objectMapper.convertValue(calls, new TypeReference<List<Map<String, Object>>>() { }));
            messages.add(assistant);

            for (JsonNode call : calls) {
                String name = call.path("function").path("name").asText();
                toolsUsed.add(name);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("role", "tool");
                result.put("tool_call_id", call.path("id").asText());
                result.put("name", name);
                result.put("content", runTool(name, call.path("function").path("arguments").asText("")));
                messages.add(result);
            }
        }
        return new AssistantResponse("That question needed more steps than I'm allowed to take. Try asking something more specific.", toolsUsed);
    }

    @Override
    public String complete(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                message("system", AssistantTools.SYSTEM_PROMPT + "\n\nToday's date: " + LocalDate.now()),
                message("user", prompt)));
        body.put("temperature", 0.3);
        body.put("max_completion_tokens", 1024);
        try {
            String text = send(body).path("choices").path(0).path("message").path("content").asText("").trim();
            return text.isEmpty() ? null : text;
        } catch (RuntimeException e) {
            log.warn("Groq completion failed: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode send(Map<String, Object> body) {
        boolean retriedToolCall = false;
        boolean retriedRateLimit = false;
        while (true) {
            try {
                JsonNode response = post(body);
                JsonNode usage = response.path("usage");
                log.info("Groq usage: {} prompt tokens ({} cached), {} completion tokens",
                        usage.path("prompt_tokens").asInt(),
                        usage.path("prompt_tokens_details").path("cached_tokens").asInt(),
                        usage.path("completion_tokens").asInt());
                return response;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                String responseBody = e.getResponseBodyAsString();
                // Open models occasionally emit a malformed tool call; one retry usually fixes it
                if (status == 400 && responseBody.contains("tool_use_failed") && !retriedToolCall) {
                    log.info("Groq returned tool_use_failed, retrying once");
                    retriedToolCall = true;
                    continue;
                }
                if (status == 429) {
                    long waitSeconds = retryAfterSeconds(e);
                    // The free tier limits tokens per minute; a short wait is better than failing the question
                    if (!retriedRateLimit && waitSeconds > 0 && waitSeconds <= MAX_RATE_LIMIT_WAIT_SECONDS) {
                        log.info("Groq rate limit reached, retrying in {}s", waitSeconds);
                        retriedRateLimit = true;
                        pause(waitSeconds * 1000 + 500);
                        continue;
                    }
                    log.warn("Groq rate limit reached (retry after {}s): {}", waitSeconds, responseBody);
                    throw AiErrors.rateLimited(waitSeconds);
                }
                if (status == 401 || status == 403) {
                    log.error("Groq rejected the API key");
                    throw AiErrors.badKey();
                }
                log.error("Groq API error {}: {}", status, responseBody);
                throw AiErrors.upstream();
            } catch (ResourceAccessException e) {
                log.error("Could not reach Groq", e);
                throw AiErrors.unreachable();
            }
        }
    }

    private JsonNode post(Map<String, Object> body) {
        return http.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Seconds Groq asks us to wait, from the retry-after header; 0 when it is missing or unreadable. */
    private static long retryAfterSeconds(HttpStatusCodeException e) {
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("retry-after");
        if (header == null) {
            return 0;
        }
        try {
            return (long) Math.ceil(Double.parseDouble(header.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /** Separate method so tests can skip the real wait. */
    void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String runTool(String name, String arguments) {
        try {
            Map<String, Object> input = arguments.isBlank() || "null".equals(arguments)
                    ? new HashMap<>()
                    : objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() { });
            return tools.run(name, input);
        } catch (Exception e) {
            log.warn("Assistant tool {} failed: {}", name, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private static Map<String, Object> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }
}
