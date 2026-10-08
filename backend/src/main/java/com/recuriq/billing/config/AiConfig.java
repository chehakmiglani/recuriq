package com.recuriq.billing.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * Claude client for the AI billing assistant.
 *
 * The assistant is optional: when no API key is configured the client is empty,
 * the assistant reports itself as disabled, and the rest of the app runs normally.
 */
@Configuration
public class AiConfig {

    @Bean
    public Optional<AnthropicClient> anthropicClient(
            @Value("${recuriq.ai.api-key:${ANTHROPIC_API_KEY:}}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(AnthropicOkHttpClient.builder().apiKey(apiKey).build());
    }
}
