package com.tomzxy.busozy.dto.response;

import java.time.OffsetDateTime;

public record VendorProfileResDTO(
        Long userId,
        String username,
        String email,
        String fullName,
        String phone,
        Long companyId,
        String companyName,
        String taxCode,
        String companyPhone,
        String companyAddress,
        OffsetDateTime createdAt) {

}
