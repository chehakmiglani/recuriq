package com.recuriq.billing.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.ai.AssistantTools;
import com.recuriq.billing.ai.LlmProvider;

class AiConfigTest {

    private final AiConfig config = new AiConfig();
    private final AssistantTools tools = mock(AssistantTools.class);

    private Optional<LlmProvider> choose(String provider, String groqKey, String anthropicKey) {
        return Optional.ofNullable(config.llmProvider(provider, groqKey, "groq-model", anthropicKey, "claude-model",
                tools, new ObjectMapper(), RestClient.builder()));
    }

    @Test
    void noKeys_disablesAi() {
        assertTrue(choose("auto", "", "").isEmpty());
    }

    @Test
    void auto_prefersGroqWhenBothKeysAreSet() {
        assertEquals("Groq", choose("auto", "g", "a").orElseThrow().displayName());
    }

    @Test
    void auto_fallsBackToClaude() {
        LlmProvider provider = choose("auto", "", "a").orElseThrow();
        assertEquals("Claude", provider.displayName());
        assertEquals("claude-model", provider.model());
    }

    @Test
    void explicitProvider_isRespected() {
        assertEquals("Claude", choose("anthropic", "g", "a").orElseThrow().displayName());
        assertEquals("Groq", choose("GROQ", "g", "").orElseThrow().displayName());
    }

    @Test
    void explicitProviderWithoutItsKey_disablesAi() {
        assertTrue(choose("groq", "", "a").isEmpty());
    }
}
