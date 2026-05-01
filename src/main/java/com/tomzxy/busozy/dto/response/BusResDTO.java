package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.BusStatus;

import java.time.OffsetDateTime;

public record BusResDTO(
        Long id,
        String companyName,
        String busTypeCode,
        String busTypeName,
        String licensePlate,
        String busNumber,
        String name,
        BusStatus status,
        OffsetDateTime createdAt) {
}
