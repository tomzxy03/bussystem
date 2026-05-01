package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.entity.Promotion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionResDTO(
        Long id,
        String code,
        String name,
        String description,
        com.tomzxy.busozy.common.enums.DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        Integer usageLimit,
        Integer usedCount,
        Integer perUserLimit,
        Boolean isActive,
        OffsetDateTime createdAt) {
}
