package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.common.enums.UserType;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.LoginReqDTO;
import com.tomzxy.busozy.dto.request.UpdateProfileReqDTO;
import com.tomzxy.busozy.dto.request.auth.RegisterReqDTO;
import com.tomzxy.busozy.dto.request.auth.RegisterVendorReqDTO;
import com.tomzxy.busozy.dto.response.AuthResDTO;
import com.tomzxy.busozy.dto.response.UserResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.exception.UnauthorizedException;
import com.tomzxy.busozy.mapper.UserMapper;
import com.tomzxy.busozy.repository.CompanyRepository;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.security.JwtService;
import com.tomzxy.busozy.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    @Value("${app.jwt.access-ttl}")
    private long accessTtl;

    @Value("${app.jwt.refresh-ttl}")
    private long refreshTtl;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:register:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResDTO register(RegisterReqDTO req, String idempotencyKey) {
        // 1. Idempotency check (busozy:{env}:idempotency:register:{key} TTL 24h)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = redisConfig.keyPrefix() + IDEMPOTENCY_PREFIX + idempotencyKey;
            Object cached = redisTemplate.opsForValue().get(idemKey);
            if (cached instanceof AuthResDTO cachedDTO) {
                log.info("Idempotency hit for register key={}", idempotencyKey);
                return cachedDTO;
            }
        }

        // 2. Uniqueness checks
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ConflictException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            throw new ConflictException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // 3. Save user
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .userType(UserType.CUSTOMER)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // 4. Generate tokens
        AuthResDTO result = generateTokens(user);

        // 5. Cache for idempotency (TTL 24h)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = redisConfig.keyPrefix() + IDEMPOTENCY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(idemKey, result, Duration.ofHours(24));
        }

        // 6. Store refresh token: busozy:{env}:auth:refresh:{userId} TTL 7d
        storeRefreshToken(user.getId(), result.refreshToken());

        log.info("User registered: userId={}", user.getId());
        return result;
    }

    @Override
    @Transactional
    public AuthResDTO registerVendor(RegisterVendorReqDTO req, String idempotencyKey) {
        // 1. Idempotency check (busozy:{env}:idempotency:register:{key} TTL 24h)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = redisConfig.keyPrefix() + IDEMPOTENCY_PREFIX + idempotencyKey;
            Object cached = redisTemplate.opsForValue().get(idemKey);
            if (cached instanceof AuthResDTO cachedDTO) {
                log.info("Idempotency hit for register key={}", idempotencyKey);
                return cachedDTO;
            }
        }

        // 2. Uniqueness checks
        if (userRepository.existsByEmail(req.getAccount().getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(req.getAccount().getUsername())) {
            throw new ConflictException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (req.getAccount().getPhone() != null
                && userRepository.existsByPhone(req.getAccount().getPhone())) {
            throw new ConflictException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (req.getTaxCode() != null && companyRepository.existsByTaxCode(req.getTaxCode())) {
            throw new ConflictException(ErrorCode.TAX_CODE_EXISTS);
        }

        Company company = new Company();
        company.setName(req.getCompanyName());
        company.setTaxCode(req.getTaxCode());
        company.setPhone(req.getCompanyPhone());
        company.setAddress(req.getAddress());
        company.setIsActive(true);
        companyRepository.save(company);

        // 3. Save user
        User user = User.builder()
                .username(req.getAccount().getUsername())
                .email(req.getAccount().getEmail())
                .passwordHash(passwordEncoder.encode(req.getAccount().getPassword()))
                .fullName(req.getAccount().getFullName())
                .phone(req.getAccount().getPhone())
                .company(company)
                .userType(UserType.VENDOR)
                .role(UserRole.VENDOR)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // 4. Generate tokens
        AuthResDTO result = generateTokens(user);

        // 5. Cache for idempotency (TTL 24h)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = redisConfig.keyPrefix() + IDEMPOTENCY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(idemKey, result, Duration.ofHours(24));
        }

        // 6. Store refresh token: busozy:{env}:auth:refresh:{userId} TTL 7d
        storeRefreshToken(user.getId(), result.refreshToken());

        log.info("Vendor registered: userId={}, companyId={}", user.getId(), company.getId());
        return result;
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResDTO login(LoginReqDTO req, String clientIp) {
        User user = userRepository.findByEmailOrUsername(req.getCredential(), req.getCredential())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED,
                    user.getBanReason() != null && !user.getBanReason().isBlank()
                            ? "Tài khoản đã bị khóa: " + user.getBanReason()
                            : ErrorCode.ACCOUNT_BANNED.getDefaultMessage());
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // Update last login info
        user.setLastLoginAt(OffsetDateTime.now());
        user.setLastLoginIp(clientIp);
        userRepository.save(user);

        AuthResDTO result = generateTokens(user);
        storeRefreshToken(user.getId(), result.refreshToken());

        log.info("User logged in: userId={}", user.getId());
        return result;
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────
    @Override
    public AuthResDTO refreshToken(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
        }

        User user = userRepository.findByEmailOrUsername(username, username)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_INVALID));

        String refreshKey = redisConfig.keyPrefix() + REFRESH_TOKEN_PREFIX + user.getId();
        Object storedToken = redisTemplate.opsForValue().get(refreshKey);

        if (storedToken == null || !refreshToken.equals(storedToken.toString())) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED,
                    user.getBanReason() != null && !user.getBanReason().isBlank()
                            ? "Tài khoản đã bị khóa: " + user.getBanReason()
                            : ErrorCode.ACCOUNT_BANNED.getDefaultMessage());
        }

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        }

        // Rotate tokens
        AuthResDTO result = generateTokens(user);
        storeRefreshToken(user.getId(), result.refreshToken());

        return result;
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────
    @Override
    public void logout(String accessToken) {
        try {
            long remaining = jwtService.getRemainingTtlMs(accessToken);
            // Use token hash as jti identifier (simple approach)
            String jti = String.valueOf(accessToken.hashCode());
            String blacklistKey = redisConfig.keyPrefix() + BLACKLIST_PREFIX + jti;
            if (remaining > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", Duration.ofMillis(remaining));
            }

            // Delete refresh token
            String username = jwtService.extractUsername(accessToken);
            userRepository.findByEmailOrUsername(username, username).ifPresent(user -> {
                String refreshKey = redisConfig.keyPrefix() + REFRESH_TOKEN_PREFIX + user.getId();
                redisTemplate.delete(refreshKey);
            });
        } catch (Exception e) {
            log.warn("Logout error (token may be expired): {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // GET MY PROFILE
    // ─────────────────────────────────────────────
    @Override
    public UserResDTO getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResDTO(user);
    }

    // ─────────────────────────────────────────────
    // UPDATE PROFILE
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public UserResDTO updateMyProfile(Long userId, UpdateProfileReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        // Check phone uniqueness (exclude current user)
        String newPhone = req.getPhone();
        if (newPhone != null && !newPhone.equals(user.getPhone())
                && userRepository.existsByPhone(newPhone)) {
            throw new ConflictException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setDateOfBirth(req.getDateOfBirth());
        user.setGender(req.getGender());
        user.setAddress(req.getAddress());
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }

        userRepository.save(user);
        return userMapper.toResDTO(user);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private AuthResDTO generateTokens(User user) {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userType", user.getUserType().name());
        claims.put("companyId", user.getCompany() != null ? user.getCompany().getId() : null);
        String accessToken = jwtService.generateToken(claims, user, accessTtl);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResDTO(
                accessToken,
                refreshToken,
                accessTtl,
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getUserType(),
                user.getCompany() != null ? user.getCompany().getId() : null);
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        String refreshKey = redisConfig.keyPrefix() + REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(refreshKey, refreshToken, Duration.ofMillis(refreshTtl));
    }
}
