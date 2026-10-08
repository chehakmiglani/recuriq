package com.recuriq.billing.service;

/**
 * Contract for a mock payment gateway used in testing and development.
 *
 * <p>Implementations simulate payment processing with support for:
 * <ul>
 *   <li>Deterministic failures via test card numbers / UPI IDs</li>
 *   <li>Configurable random failure rates for realism</li>
 * </ul>
 */
public interface MockPaymentGateway {

    /**
     * Processes a mock charge.
     *
     * @param gatewayToken the payment method token (may contain a test decline pattern)
     * @param amountMinor  amount in minor currency units
     * @param currency     currency code (e.g., INR, USD)
     * @return a mock gateway reference on success
     * @throws com.recuriq.billing.exception.CustomException with PAYMENT_FAILED if the charge is declined
     */
    String charge(String gatewayToken, long amountMinor, String currency);

    /**
     * Processes a mock refund.
     *
     * @param originalGatewayRef the original charge gateway reference
     * @param amountMinor        refund amount in minor currency units
     * @param currency           currency code (e.g., INR, USD)
     * @return a mock refund reference on success
     */
    String refund(String originalGatewayRef, long amountMinor, String currency);
}
