package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returned immediately after POST /payments/initiate.
 * FE should redirect to paymentUrl if non-null (BANK_TRANSFER/VNPAY/MoMo).
 * If paymentUrl is null, payment is complete (COD).
 */
public record PaymentInitiateResDTO(
        Long paymentId,
        UUID bookingCode,
        BigDecimal amount,
        String paymentUrl, // null for COD; mock URL for BANK_TRANSFER sandbox
        String qrCodeData, // null until MoMo/VNPAY integrated
        String directPayUrl // null until deep-link integrated
) {
}
