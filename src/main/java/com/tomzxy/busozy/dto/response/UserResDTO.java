package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.Gender;
import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.common.enums.UserType;

import java.time.OffsetDateTime;

public record UserResDTO(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        Gender gender,
        UserRole role,
        UserType userType,
        UserStatus status,
        Long companyId,
        String companyName,
        OffsetDateTime createdAt) {
}
