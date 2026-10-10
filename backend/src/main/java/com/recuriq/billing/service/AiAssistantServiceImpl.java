package com.recuriq.billing.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.recuriq.billing.ai.AssistantTools;
import com.recuriq.billing.ai.LlmProvider;
import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;
import com.recuriq.billing.exception.CustomException;

import lombok.RequiredArgsConstructor;

/**
 * AI billing analyst. The model never sees the database: it can only call the
 * read-only tools in {@link AssistantTools}, and writes its answer from their results.
 * The model provider (Groq or Claude) is chosen in AiConfig.
 */
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final Optional<LlmProvider> provider;

    @Override
    public boolean isEnabled() {
        return provider.isPresent();
    }

    @Override
    public String providerName() {
        return provider.map(p -> p.displayName() + " · " + p.model()).orElse(null);
    }

    @Override
    public AssistantResponse ask(AssistantRequest request) {
        LlmProvider llm = provider.orElseThrow(() -> new CustomException(
                "The AI assistant is not configured. Set GROQ_API_KEY (free) or ANTHROPIC_API_KEY and restart the backend.",
                HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED"));
        return llm.ask(AssistantTools.alternatingHistory(request.getHistory()), request.getQuestion());
    }

    @Override
    public String writeBriefing(String snapshotJson) {
        return provider.map(llm -> llm.complete(AssistantTools.BRIEFING_PROMPT + snapshotJson)).orElse(null);
    }
}
