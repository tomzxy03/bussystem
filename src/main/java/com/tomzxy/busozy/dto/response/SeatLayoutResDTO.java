package com.tomzxy.busozy.dto.response;

import java.util.Map;

public record SeatLayoutResDTO(
        Long id,
        Long busTypeId,
        String name,
        Map<String, Object> layoutData,
        Integer totalSeats) {
}
