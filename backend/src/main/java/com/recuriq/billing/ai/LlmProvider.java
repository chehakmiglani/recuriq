package com.recuriq.billing.ai;

import java.util.List;

import com.recuriq.billing.dto.ai.AssistantRequest;
import com.recuriq.billing.dto.ai.AssistantResponse;

/** A model provider that can run the analyst's tool-calling loop and write single replies. */
public interface LlmProvider {

    /** Shown in the UI, e.g. "Groq" or "Claude". */
    String displayName();

    String model();

    /** Answers the question, calling tools as needed. Throws CustomException on API errors. */
    AssistantResponse ask(List<AssistantRequest.ChatTurn> history, String question);

    /** One reply with no tools; returns null when the model gives nothing usable. */
    String complete(String prompt);
}
