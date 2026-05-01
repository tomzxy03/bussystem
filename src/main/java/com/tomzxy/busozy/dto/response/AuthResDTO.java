package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserType;

public record AuthResDTO(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        String username,
        UserRole role,
        UserType userType,
        Long companyId
) {
}
