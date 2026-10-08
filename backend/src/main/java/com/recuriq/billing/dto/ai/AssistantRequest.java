package com.recuriq.billing.dto.ai;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssistantRequest {

    @NotBlank
    @Size(max = 2000)
    private String question;

    // Earlier turns of this chat, oldest first, so follow-up questions have context
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role;      // "user" or "assistant"
        private String content;
    }
}
