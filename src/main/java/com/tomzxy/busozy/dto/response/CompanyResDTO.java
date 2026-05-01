package com.tomzxy.busozy.dto.response;

import java.time.OffsetDateTime;

public record CompanyResDTO(
        Long id,
        String name,
        String taxCode,
        String phone,
        String address,
        Boolean isActive,
        OffsetDateTime createdAt) {
}
