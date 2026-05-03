package com.tomzxy.busozy.payment;

import com.tomzxy.busozy.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategy interface for payment gateway providers.
 * Implement this to add VNPAY, MoMo, etc. without touching PaymentService.
 *
 * Current implementations:
 * - MockGatewayProvider (BANK_TRANSFER — sandbox)
 * - CodGatewayProvider (COD — instant confirm)
 */
public interface PaymentGatewayProvider {

    /**
     * Returns true if this provider handles the given method code (e.g.,
     * "BANK_TRANSFER").
     */
    boolean supports(String methodCode);

    /**
     * Initiates the payment and returns URLs for FE redirect / QR display.
     *
     * @param payment   the persisted Payment entity (id is available)
     * @param returnUrl where gateway should redirect after payment
     */
    GatewayInitiateResponse initiate(Payment payment, String returnUrl);

    /**
     * Verifies HMAC signature of incoming webhook params.
     * 
     * @return true if signature is valid (or mock — always true)
     */
    boolean verifySignature(Map<String, String> params);

    /**
     * Processes the callback params and returns the outcome.
     */
    PaymentProcessResult processCallback(Map<String, String> params);

    /**
     * Processes a refund request for a previously paid payment.
     */
    GatewayRefundResponse refund(Payment payment, BigDecimal amount, String reason);
}
