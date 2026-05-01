package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.entity.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResDTO(
        Long paymentId,
        UUID bookingCode,
        String methodCode,
        BigDecimal amount,
        com.tomzxy.busozy.common.enums.PaymentTransactionStatus status,
        String gatewayTransactionId) {
}
