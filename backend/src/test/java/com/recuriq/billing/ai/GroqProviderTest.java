package com.recuriq.billing.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.ai.AssistantResponse;
import com.recuriq.billing.exception.CustomException;

class GroqProviderTest {

    private static final String URL = GroqProvider.BASE_URL + "/chat/completions";

    private MockRestServiceServer server;
    private AssistantTools tools;
    private GroqProvider provider;
    private long pausedMillis;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        tools = mock(AssistantTools.class);
        provider = new GroqProvider(builder, "test-key", tools, new ObjectMapper(), "openai/gpt-oss-120b") {
            @Override
            void pause(long millis) {
                pausedMillis += millis;   // no real waiting in tests
            }
        };
    }

    @Test
    void ask_runsRequestedToolAndReturnsFinalAnswer() throws Exception {
        when(tools.run(eq("get_churn_risk"), anyMap())).thenReturn("{\"highRisk\":2}");

        server.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.tools[0].type").value("function"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Who might cancel?"))
                .andRespond(withSuccess("""
                        {"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                          "tool_calls":[{"id":"call_1","type":"function",
                            "function":{"name":"get_churn_risk","arguments":"{\\"level\\":\\"HIGH\\",\\"limit\\":5}"}}]}}]}
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(URL))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[2].tool_calls[0].id").value("call_1"))
                .andExpect(jsonPath("$.messages[3].role").value("tool"))
                .andExpect(jsonPath("$.messages[3].tool_call_id").value("call_1"))
                .andExpect(jsonPath("$.messages[3].content").value("{\"highRisk\":2}"))
                .andRespond(withSuccess("""
                        {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"Two customers are high risk."}}]}
                        """, MediaType.APPLICATION_JSON));

        AssistantResponse response = provider.ask(List.of(), "Who might cancel?");

        assertEquals("Two customers are high risk.", response.getAnswer());
        assertEquals(List.of("get_churn_risk"), response.getToolsUsed());
        verify(tools).run("get_churn_risk", java.util.Map.of("level", "HIGH", "limit", 5));
        server.verify();
    }

    @Test
    void ask_sendsToolErrorsBackToTheModel() throws Exception {
        when(tools.run(anyString(), anyMap())).thenThrow(new IllegalArgumentException("Unknown tool: nope"));

        server.expect(once(), requestTo(URL)).andRespond(withSuccess("""
                {"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"c","type":"function",
                  "function":{"name":"nope","arguments":""}}]}}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URL))
                .andExpect(jsonPath("$.messages[3].content").value("Error: Unknown tool: nope"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"That data isn't available."}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("That data isn't available.", provider.ask(List.of(), "x").getAnswer());
        server.verify();
    }

    @Test
    void ask_retriesOnceWhenModelEmitsMalformedToolCall() {
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"tool_use_failed\"}}"));
        server.expect(once(), requestTo(URL)).andRespond(withSuccess("""
                {"choices":[{"message":{"role":"assistant","content":"Done."}}]}
                """, MediaType.APPLICATION_JSON));

        assertEquals("Done.", provider.ask(List.of(), "x").getAnswer());
        server.verify();
    }

    @Test
    void ask_mapsInvalidKeyTo503() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        CustomException e = assertThrows(CustomException.class, () -> provider.ask(List.of(), "x"));
        assertEquals("AI_AUTH_FAILED", e.getErrorCode());
    }

    @Test
    void ask_mapsRateLimitTo429() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        CustomException e = assertThrows(CustomException.class, () -> provider.ask(List.of(), "x"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertEquals(0, pausedMillis);
    }

    @Test
    void ask_waitsAndRetriesWhenRateLimitResetsSoon() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("retry-after", "7.4"));
        server.expect(once(), requestTo(URL)).andRespond(withSuccess("""
                {"choices":[{"message":{"role":"assistant","content":"Done."}}]}
                """, MediaType.APPLICATION_JSON));

        assertEquals("Done.", provider.ask(List.of(), "x").getAnswer());
        assertEquals(8500, pausedMillis);   // 7.4s rounded up to 8s, plus a small margin
        server.verify();
    }

    @Test
    void ask_reportsWaitTimeWhenRateLimitResetIsFarAway() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("retry-after", "600"));

        CustomException e = assertThrows(CustomException.class, () -> provider.ask(List.of(), "x"));
        assertTrue(e.getMessage().contains("10 minutes"), e.getMessage());
        assertEquals(0, pausedMillis);
        server.verify();
    }

    @Test
    void complete_returnsTextOrNullOnFailure() {
        server.expect(once(), requestTo(URL))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"  Revenue is up.  "}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URL)).andRespond(withServerError());

        assertEquals("Revenue is up.", provider.complete("brief"));
        assertNull(provider.complete("brief"));
        server.verify();
    }
}
