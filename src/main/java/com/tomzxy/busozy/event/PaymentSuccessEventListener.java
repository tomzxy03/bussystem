package com.tomzxy.busozy.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async listener for PaymentSuccessEvent.
 * Phase 11 will replace the log stub with email/SMS/push notification.
 * 
 * @Async requires @EnableAsync (added to BusozyApplication).
 */
@Component
@RequiredArgsConstructor
public class PaymentSuccessEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSuccessEventListener.class);

    @Async
    @EventListener
    public void handle(PaymentSuccessEvent event) {
        var payment = event.getPayment();
        log.info("[NOTIFICATION-STUB] Payment PAID: paymentId={}, booking={}, amount={}",
                payment.getId(),
                payment.getBooking().getBookingCode(),
                payment.getAmount());
        // Phase 11: inject NotificationService.sendPaymentConfirmation(payment)
    }
}
