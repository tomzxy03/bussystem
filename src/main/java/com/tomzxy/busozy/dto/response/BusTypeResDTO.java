package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record BusTypeResDTO(
        Long id,
        String code,
        String name,
        String description,
        Map<String, Object> amenities,
        BigDecimal basePricePerKm,
        Boolean isActive) {
}
