package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.NotificationChannel;
import com.tomzxy.busozy.common.enums.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.Map;

public record NotificationResDTO(
        Long id,
        NotificationChannel channel,
        String title,
        String content,
        Map<String, Object> metadata,
        NotificationStatus status,
        OffsetDateTime sentAt,
        OffsetDateTime readAt,
        OffsetDateTime createdAt) {
}
