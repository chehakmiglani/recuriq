package com.recuriq.billing.service;

import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;

public interface AiAssistantService {

    /** True when an Anthropic API key is configured. */
    boolean isEnabled();

    /** Answers a plain-English billing question using live data from the analytics services. */
    AssistantResponse ask(AssistantRequest request);

    /** Writes a short plain-English briefing from a JSON snapshot of the business. */
    String writeBriefing(String snapshotJson);
}
