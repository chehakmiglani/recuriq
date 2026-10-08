package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.SupportMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSupportServiceImplTest {

    private CustomerSupportServiceImpl customerSupportService;

    @BeforeEach
    void setUp() {
        customerSupportService = new CustomerSupportServiceImpl();
    }

    @Test
    void testGetFAQs_ReturnsNonEmptyList() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        assertNotNull(faqs);
        assertFalse(faqs.isEmpty());
    }

    @Test
    void testGetFAQs_ReturnsCorrectCount() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        assertEquals(8, faqs.size());
    }

    @Test
    void testGetFAQs_FirstFAQ_IsAccountCategory() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        assertEquals("ACCOUNT", faqs.get(0).get("category"));
    }

    @ParameterizedTest
    @CsvSource({"BILLING", "PAYMENT", "FEATURES"})
    void testGetFAQs_ContainsCategory(String category) {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        boolean hasCategory = faqs.stream()
                .anyMatch(faq -> category.equals(faq.get("category")));
        assertTrue(hasCategory);
    }

    @Test
    void testGetFAQs_AllEntriesHaveRequiredKeys() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        for (Map<String, String> faq : faqs) {
            assertTrue(faq.containsKey("question"), "FAQ missing 'question' key");
            assertTrue(faq.containsKey("answer"), "FAQ missing 'answer' key");
            assertTrue(faq.containsKey("category"), "FAQ missing 'category' key");
        }
    }

    @Test
    void testGetFAQs_NoEmptyValues() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        for (Map<String, String> faq : faqs) {
            assertFalse(faq.get("question").isBlank(), "FAQ has blank question");
            assertFalse(faq.get("answer").isBlank(), "FAQ has blank answer");
            assertFalse(faq.get("category").isBlank(), "FAQ has blank category");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "BILLING, 3",
            "ACCOUNT, 2",
            "FEATURES, 2",
            "PAYMENT, 1"
    })
    void testGetFAQs_CategoryCount(String category, long expectedCount) {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        long actualCount = faqs.stream()
                .filter(faq -> category.equals(faq.get("category")))
                .count();
        assertEquals(expectedCount, actualCount);
    }

    @ParameterizedTest
    @CsvSource({"cancel", "pause"})
    void testGetFAQs_QuestionTopicExists(String topic) {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        boolean hasQuestion = faqs.stream()
                .anyMatch(faq -> faq.get("question").toLowerCase().contains(topic));
        assertTrue(hasQuestion, "Expected a FAQ about " + topic);
    }

    @ParameterizedTest
    @MethodSource("sendSupportMessageArgs")
    void testSendSupportMessage_DoesNotThrow(String email, SupportMessageDTO dto) {
        assertDoesNotThrow(() -> customerSupportService.sendSupportMessage(email, dto));
    }

    private static Stream<Arguments> sendSupportMessageArgs() {
        SupportMessageDTO standard = new SupportMessageDTO();
        standard.setSubject("Test Subject");
        standard.setMessage("Test message body");
        standard.setCategory("BILLING");

        SupportMessageDTO populated = new SupportMessageDTO();
        populated.setSubject("Billing Issue");
        populated.setMessage("I was charged twice for my subscription this month.");
        populated.setCategory("BILLING");

        return Stream.of(
                Arguments.of("user@example.com", standard),
                Arguments.of(null, standard),
                Arguments.of("user@example.com", null),
                Arguments.of("customer@test.com", populated)
        );
    }

    @Test
    void testGetFAQs_ImmutableList() {
        List<Map<String, String>> faqs = customerSupportService.getFAQs();
        Map<String, String> entry = Map.of("question", "test", "answer", "test", "category", "test");
        assertThrows(UnsupportedOperationException.class, () -> faqs.add(entry));
    }

    @Test
    void testGetFAQs_ConsistentResults() {
        List<Map<String, String>> firstCall = customerSupportService.getFAQs();
        List<Map<String, String>> secondCall = customerSupportService.getFAQs();
        assertEquals(firstCall.size(), secondCall.size());
        for (int i = 0; i < firstCall.size(); i++) {
            assertEquals(firstCall.get(i).get("question"), secondCall.get(i).get("question"));
            assertEquals(firstCall.get(i).get("answer"), secondCall.get(i).get("answer"));
            assertEquals(firstCall.get(i).get("category"), secondCall.get(i).get("category"));
        }
    }
}
