package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.common.enums.UserType;

import java.time.OffsetDateTime;

public record AdminUserResDTO(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        UserRole role,
        UserType userType,
        UserStatus status,
        Boolean isBanned,
        String banReason,
        Long companyId,
        String companyName,
        OffsetDateTime createdAt) {
}
