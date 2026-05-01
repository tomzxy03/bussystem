package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.PaymentInitiateReqDTO;
import com.tomzxy.busozy.dto.response.PaymentInitiateResDTO;
import com.tomzxy.busozy.dto.response.PaymentResDTO;
import com.tomzxy.busozy.entity.Payment;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    /** GET /payment-methods — active methods only (public) */
    List<com.tomzxy.busozy.entity.PaymentMethod> getActiveMethods();

    /** POST /payments/initiate — 6-step initiation flow */
    PaymentInitiateResDTO initiatePayment(Long userId, PaymentInitiateReqDTO req);

    /**
     * POST /payments/callback/{provider} — strict webhook handler.
     * Verifies signature, enforces idempotency, updates Payment + Booking.
     */
    void handleWebhook(String provider, Map<String, String> params);

    /** POST /payments/{id}/cancel — only PENDING payments can be cancelled */
    PaymentResDTO cancelPayment(Long userId, Long paymentId);

    /** GET /payments/{id} — status check (owner or admin) */
    PaymentResDTO getPaymentStatus(Long userId, Long paymentId);
}
