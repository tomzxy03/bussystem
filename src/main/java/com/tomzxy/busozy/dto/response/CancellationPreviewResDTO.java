package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CancellationPreviewResDTO(
        Long bookingId,
        String bookingCode,
        BigDecimal originalAmount,
        BigDecimal refundAmount,
        Double refundPercentage,
        String policyDescription,
        OffsetDateTime departureTime) {
}
