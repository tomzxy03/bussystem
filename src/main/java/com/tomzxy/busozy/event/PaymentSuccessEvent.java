package com.tomzxy.busozy.event;

import com.tomzxy.busozy.entity.Payment;

/**
 * Published after a payment transitions to PAID.
 * Async listener sends confirmation email/SMS (stub until Phase 11).
 */
public class PaymentSuccessEvent {

    private final Payment payment;

    public PaymentSuccessEvent(Payment payment) {
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }
}
