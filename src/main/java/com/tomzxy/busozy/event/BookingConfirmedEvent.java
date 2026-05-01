package com.tomzxy.busozy.event;

public record BookingConfirmedEvent(Long bookingId, Long promotionId, Long userId) {
}
