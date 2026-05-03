package com.tomzxy.busozy.event;

public record PaymentSuccessEvent(
        Long paymentId,
        Long bookingId,
        Long userId,
        String bookingCode) {
}
