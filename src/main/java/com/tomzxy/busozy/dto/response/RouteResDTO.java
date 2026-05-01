package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RouteResDTO(
        Long id,
        String code,
        String name,
        String companyName,
        BigDecimal distanceKm,
        Integer durationMinutes,
        Boolean isActive,
        OffsetDateTime createdAt) {
}
