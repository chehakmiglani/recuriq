package com.recuriq.billing.service;

import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;

public interface AiAssistantService {

    /** True when an AI provider (Groq or Claude) is configured. */
    boolean isEnabled();

    /** e.g. "Groq · openai/gpt-oss-120b"; null when disabled. */
    String providerName();

    /** Answers a plain-English billing question using live data from the analytics services. */
    AssistantResponse ask(AssistantRequest request);

    /** Writes a short plain-English briefing from a JSON snapshot of the business. */
    String writeBriefing(String snapshotJson);
}
