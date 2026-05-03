package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.BanUserReqDTO;
import com.tomzxy.busozy.dto.response.AdminUserResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    @Override
    public Page<AdminUserResDTO> getUsers(String search, UserStatus status, UserRole role, Boolean banned, Pageable pageable) {
        return userRepository.searchAdminUsers(normalize(search), status, role, banned, pageable)
                .map(this::toAdminUserRes);
    }

    @Override
    @Transactional
    public AdminUserResDTO updateBanStatus(Long userId, BanUserReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        boolean banned = Boolean.TRUE.equals(req.banned());
        if (banned && (req.reason() == null || req.reason().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Lý do khóa tài khoản là bắt buộc");
        }

        user.setIsBanned(banned);
        user.setBanReason(banned ? req.reason().trim() : null);
        user.setStatus(banned ? UserStatus.BLOCKED : UserStatus.ACTIVE);
        userRepository.save(user);
        redisTemplate.delete(redisConfig.keyPrefix() + REFRESH_TOKEN_PREFIX + user.getId());
        return toAdminUserRes(user);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AdminUserResDTO toAdminUserRes(User user) {
        return new AdminUserResDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getUserType(),
                user.getStatus(),
                user.getIsBanned(),
                user.getBanReason(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getName() : null,
                user.getCreatedAt());
    }
}
