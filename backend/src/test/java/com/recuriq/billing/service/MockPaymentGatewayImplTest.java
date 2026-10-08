package com.recuriq.billing.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.recuriq.billing.exception.CustomException;
class MockPaymentGatewayImplTest {
    private MockPaymentGatewayImpl mockPaymentGateway;
    @BeforeEach
    void setUp() {
        mockPaymentGateway = new MockPaymentGatewayImpl();
    }
    @Test
    void testCharge_Success() {
        String ref = mockPaymentGateway.charge("valid_token", 1000L, "USD");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
    @Test
    void testCharge_DeclineCard() {
        assertThrows(CustomException.class, () ->
            mockPaymentGateway.charge("decline_token", 1000L, "USD")
        );
    }
    @Test
    void testCharge_DeclineUpi() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("decline@upi", 1000L, "USD")
        );
    }
    @Test
    void testCharge_NullToken() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge(null, 1000L, "USD")
        );
    }
    @Test
    void testRefund_Success() {
        String ref = mockPaymentGateway.refund("mock_charge_123", 500L, "USD");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_refund_"));
    }
    @Test
    void testCharge_SuccessTokenBypass() {
        String ref = mockPaymentGateway.charge("success_token_123", 1000L, "INR");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
    @Test
    void testCharge_ValidTokenBypass() {
        String ref = mockPaymentGateway.charge("valid_card_456", 2000L, "EUR");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
    @Test
    void testCharge_StripeDeclineCard() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("4000000000000002", 1000L, "USD")
        );
    }
    @Test
    void testCharge_StripeDeclineCard2() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("4000000000009995", 1000L, "USD")
        );
    }
    @Test
    void testCharge_StripeDeclineCard3() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("4000000000000127", 1000L, "USD")
        );
    }
    @Test
    void testCharge_FailUpi() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("fail@upi", 1000L, "INR")
        );
    }
    @Test
    void testCharge_ZeroUpi() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("0000000000@upi", 1000L, "INR")
        );
    }
    @Test
    void testCharge_DeclineToken_CaseInsensitive() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("DeClInE_token", 1000L, "USD")
        );
    }
    @Test
    void testCharge_Upi_CaseInsensitive() {
        assertThrows(CustomException.class, () ->
            mockPaymentGateway.charge("FAIL@UPI", 1000L, "INR")
        );
    }
    @Test
    void testCharge_ValidMixedCaseBypass() {
        String ref = mockPaymentGateway.charge("VaLiD_CaRd", 1000L, "USD");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
    @Test
    void testCharge_SuccessMixedCaseBypass() {
        String ref = mockPaymentGateway.charge("SuCcEsS_token", 1000L, "USD");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
    @Test
    void testRefund_WithDifferentCurrency() {
        String ref = mockPaymentGateway.refund("mock_charge_999", 2500L, "INR");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_refund_"));
    }
    @Test
    void testCharge_DeclineTokenEmbeddedInString() {
        assertThrows(CustomException.class, ()->
            mockPaymentGateway.charge("card_decline_test_123", 1000L, "USD")
        );
    }

    @Test
    void testCharge_RandomSuccess() {
        // "success" tokens skip the gateway's 20% random decline, keeping this test deterministic
        MockPaymentGatewayImpl gateway =
                new MockPaymentGatewayImpl();
        String ref = gateway.charge("success_token", 1000L, "USD");
        assertNotNull(ref);
        assertTrue(ref.startsWith("mock_charge_"));
    }
}