package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CancellationResDTO(
        Long cancellationId,
        String bookingCode,
        BigDecimal refundAmount,
        RefundStatus refundStatus,
        String cancelReason,
        OffsetDateTime cancelTime,
        String cancelledByUsername) {
}
