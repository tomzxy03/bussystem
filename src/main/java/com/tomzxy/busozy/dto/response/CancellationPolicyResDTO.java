package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CancellationPolicyResDTO(
        Long id,
        Long companyId,
        String companyName,
        Long routeId,
        String routeName,
        Integer hoursBeforeDeparture,
        BigDecimal refundPercentage,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
