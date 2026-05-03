package com.tomzxy.busozy.dto.response;

import java.time.OffsetDateTime;

public record ReviewResDTO(
        Long id,
        Long userId,
        String username,
        Long tripId,
        String routeName,
        Integer rating,
        String comment,
        Boolean isVerified,
        OffsetDateTime createdAt) {
}
