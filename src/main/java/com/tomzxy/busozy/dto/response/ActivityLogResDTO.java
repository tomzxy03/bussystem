package com.tomzxy.busozy.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record ActivityLogResDTO(
        Long id,
        Long userId,
        String username,
        String action,
        String entityType,
        Long entityId,
        Map<String, Object> details,
        String ipAddress,
        OffsetDateTime createdAt) {
}
