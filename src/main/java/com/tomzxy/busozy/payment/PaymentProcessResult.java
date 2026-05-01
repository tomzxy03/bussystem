package com.tomzxy.busozy.payment;

import java.util.Map;

/**
 * Result from PaymentGatewayProvider.processCallback() — tells PaymentService
 * whether
 * the transaction succeeded and provides the canonical transactionId.
 */
public record PaymentProcessResult(
        boolean success,
        String transactionId,
        Map<String, String> rawParams) {
}
