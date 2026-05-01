package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;

/**
 * Returned by POST /promotions/validate.
 * isValid=false means the promotion cannot be applied — check message for
 * reason.
 * FE should display discountAmount and finalPrice as a preview before payment.
 */
public record PromotionValidateResDTO(
        boolean isValid,
        String promotionCode,
        String promotionName,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        String message) {
}
