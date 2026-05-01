package com.tomzxy.busozy.payment;

import com.tomzxy.busozy.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * COD (Cash on Delivery) gateway — confirms payment instantly at booking time.
 * No redirect URL is needed; PaymentService handles the status transition
 * internally.
 */
@Component
public class CodGatewayProvider implements PaymentGatewayProvider {

    @Override
    public boolean supports(String methodCode) {
        return "COD".equals(methodCode);
    }

    @Override
    public GatewayInitiateResponse initiate(Payment payment, String returnUrl) {
        // COD has no external URL — all null; PaymentService confirms synchronously
        return new GatewayInitiateResponse(null, null, null);
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        return true; // No external callback for COD
    }

    @Override
    public PaymentProcessResult processCallback(Map<String, String> params) {
        // COD is always treated as successful at initiation
        return new PaymentProcessResult(true, UUID.randomUUID().toString(), params);
    }
}
