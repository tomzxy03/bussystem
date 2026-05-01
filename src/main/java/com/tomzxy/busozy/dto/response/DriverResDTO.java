package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.UserStatus;

import java.time.OffsetDateTime;

public record DriverResDTO(
        Long id,
        CompanySummary company,
        String fullName,
        String phone,
        String licenseNumber,
        UserStatus status,
        OffsetDateTime createdAt) {
    /**
     * Nested summary to avoid exposing full Company details in every driver
     * response.
     */
    public record CompanySummary(Long id, String name) {
    }
}
