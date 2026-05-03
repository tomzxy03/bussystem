package com.tomzxy.busozy.event;

import java.math.BigDecimal;

public record RefundRequestedEvent(
        Long cancellationId,
        Long bookingId,
        BigDecimal refundAmount) {
}
