package com.recuriq.billing.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.ai.AssistantTools;
import com.recuriq.billing.ai.ClaudeProvider;
import com.recuriq.billing.ai.GroqProvider;
import com.recuriq.billing.ai.LlmProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * Chooses the model provider for the AI analyst.
 *
 * recuriq.ai.provider = auto (default) uses Groq when GROQ_API_KEY is set, otherwise Claude
 * when ANTHROPIC_API_KEY is set. With no key the AI features are switched off and the rest
 * of the app runs normally.
 *
 * The bean is null when AI is off; Spring then injects Optional.empty() into Optional<LlmProvider>.
 */
@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public LlmProvider llmProvider(
            @Value("${recuriq.ai.provider:auto}") String provider,
            @Value("${recuriq.ai.groq.api-key:}") String groqKey,
            @Value("${recuriq.ai.groq.model:openai/gpt-oss-120b}") String groqModel,
            @Value("${recuriq.ai.anthropic.api-key:}") String anthropicKey,
            @Value("${recuriq.ai.anthropic.model:claude-opus-5-5}") String anthropicModel,
            AssistantTools tools,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {

        String choice = provider.trim().toLowerCase();
        boolean useGroq = choice.equals("groq") || (choice.equals("auto") && !groqKey.isBlank());
        boolean useClaude = choice.equals("anthropic") || choice.equals("claude")
                || (choice.equals("auto") && groqKey.isBlank() && !anthropicKey.isBlank());

        if (useGroq && !groqKey.isBlank()) {
            SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
            timeouts.setConnectTimeout(Duration.ofSeconds(10));
            timeouts.setReadTimeout(Duration.ofSeconds(90));
            log.info("AI analyst enabled: Groq ({})", groqModel);
            return new GroqProvider(restClientBuilder.requestFactory(timeouts), groqKey, tools, objectMapper, groqModel);
        }
        if (useClaude && !anthropicKey.isBlank()) {
            log.info("AI analyst enabled: Claude ({})", anthropicModel);
            return new ClaudeProvider(AnthropicOkHttpClient.builder().apiKey(anthropicKey).build(), tools, anthropicModel);
        }
        if (!choice.equals("auto")) {
            log.warn("recuriq.ai.provider is '{}' but its API key is not set; AI features are off", provider);
        } else {
            log.info("No AI API key set; AI features are off");
        }
        return null;
    }
}
