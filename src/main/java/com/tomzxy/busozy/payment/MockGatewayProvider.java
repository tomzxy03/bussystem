package com.tomzxy.busozy.payment;

import com.tomzxy.busozy.entity.Payment;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sandbox Mock for BANK_TRANSFER.
 * Returns a fake paymentUrl so FE can test the redirect flow end-to-end.
 * When a real merchant account is available:
 * 1. Create VnpayGatewayProvider / MomoGatewayProvider with @Primary.
 * 2. Remove @Primary from this class.
 */
@Component
@Primary
public class MockGatewayProvider implements PaymentGatewayProvider {

    @Override
    public boolean supports(String methodCode) {
        return "BANK_TRANSFER".equals(methodCode);
    }

    @Override
    public GatewayInitiateResponse initiate(Payment payment, String returnUrl) {
        // Generate a deterministic mock payment URL for FE to display/redirect
        String mockUrl = "https://mock-pay.local/" + payment.getId()
                + "?returnUrl=" + returnUrl
                + "&ref=" + payment.getId();
        return new GatewayInitiateResponse(mockUrl, null, null);
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        // Mock always passes — real gateways will compute HMAC-SHA256
        return true;
    }

    @Override
    public PaymentProcessResult processCallback(Map<String, String> params) {
        // Mock success — real gateways parse response code from params
        String txId = params.getOrDefault("transactionId", UUID.randomUUID().toString());
        boolean success = !"FAILED".equalsIgnoreCase(params.getOrDefault("resultCode", "SUCCESS"));
        return new PaymentProcessResult(success, txId, params);
    }

    @Override
    public GatewayRefundResponse refund(Payment payment, BigDecimal amount, String reason) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("provider", "BANK_TRANSFER");
        raw.put("status", "COMPLETED");
        raw.put("amount", amount);
        raw.put("reason", reason);
        raw.put("paymentId", payment.getId());
        return new GatewayRefundResponse("MOCK-REFUND-" + payment.getId() + "-" + UUID.randomUUID(), raw);
    }
}
