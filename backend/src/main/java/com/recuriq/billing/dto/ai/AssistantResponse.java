package com.recuriq.billing.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantResponse {
    private String answer;
    private List<String> toolsUsed;   // which data sources the assistant looked at
}
