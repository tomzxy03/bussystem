<task>

---

# 🔧 `01_auth.md` (Đã chuẩn hóa)

```markdown
# Module 01 – Authentication & Authorization
Prerequisite: `00_infrastructure` hoàn tất. Đọc `global_standards.md` trước khi code.

## Database Tables Liên Quan
`users` (id, username, email, password_hash, full_name, phone, date_of_birth, gender, avatar_url, address, status, email_verified_at, last_login_at, last_login_ip, deleted_at)

## Entities Cần Tạo
### `User` (extends `BaseEntity`, implements `UserDetails`)
- Map đầy đủ cột trong `users`
- `status`: `enum UserStatus { ACTIVE, INACTIVE, BLOCKED }`
- `gender`: `enum Gender { MALE, FEMALE, OTHER }`
- `@SQLRestriction("deleted_at IS NULL")` kế thừa từ `BaseEntity`
- Implement `getAuthorities()`: trả về `ROLE_USER` mặc định (có thể mở rộng sau)

## DTOs Cần Tạo
### `dto/request/` (class + Bean Validation)
- `RegisterReqDTO.java`: `@NotBlank @Size(3,50) username`, `@Email @NotBlank email`, `@NotBlank @Size(min=8) password`, `@NotBlank fullName`, `@Pattern(regexp="^0[0-9]{9,10}$") phone`
- `LoginReqDTO.java`: `@NotBlank credential` (username/email), `@NotBlank password`
- `UpdateProfileReqDTO.java`: `@NotBlank fullName`, `@Pattern(...) phone`, `@Past dateOfBirth`, `@NotNull gender`, `address`

### `dto/response/` (Java `record`)
- `AuthResDTO.java`: `accessToken`, `refreshToken`, `expiresIn` (long ms)
- `UserResDTO.java`: `id`, `username`, `email`, `fullName`, `phone`, `gender`, `status`, `createdAt`

## Service Contract
```java
public interface AuthService {
    AuthResDTO register(RegisterReqDTO req, String idempotencyKey);
    AuthResDTO login(LoginReqDTO req, String clientIp);
    AuthResDTO refreshToken(String refreshToken);
    void logout(String accessToken);
    UserResDTO getMyProfile(Long userId);
    UserResDTO updateMyProfile(Long userId, UpdateProfileReqDTO req);
}
```

## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Đăng ký |
| POST | `/api/v1/auth/login` | Public | Đăng nhập |
| POST | `/api/v1/auth/refresh` | Public | Refresh token |
| POST | `/api/v1/auth/logout` | Bearer | Đăng xuất |
| GET | `/api/v1/auth/me` | Bearer | Lấy profile |
| PUT | `/api/v1/auth/me` | Bearer | Cập nhật profile |

## Business Logic
1. Register:
  Check Idempotency-Key trong Redis. Nếu tồn tại → return cached response.
  Hash password với BCryptPasswordEncoder.
  Check unique username/email (ignore soft-deleted nếu cần).
  Save user → generate tokens → return.
2. Login:
  Validate credentials → check status == ACTIVE.
  Update lastLoginAt, lastLoginIp.
  Generate JWT + store refresh token in Redis (busozy:{env}:auth:refresh:{userId}).
3. Refresh: Validate refresh token signature + existence in Redis → rotate tokens.
4. Logout: Blacklist access token in Redis (busozy:{env}:auth:blacklist:{jti}, TTL = remaining life). Delete refresh token.
5. Update Profile: @Transactional. Check phone uniqueness.

### **Redis Keys**:
- Refresh token: busozy:{env}:auth:refresh:{userId} → TTL 7d
- Blacklisted access: busozy:{env}:auth:blacklist:{jti} → TTL auto-expire
- Idempotency cache: busozy:{env}:idempotency:register:{key} → TTL 24h

## Validation
- username: 3-50 chars, alphanumeric + underscore, no spaces.
- password: min 8, recommend @Pattern for strength.
- phone: VN format 0[3|5|7|8|9][0-9]{8}.
- SecurityConfig update: Keep /api/v1/auth/** as permitAll(). Áp dụng @PreAuthorize nếu cần role-specific endpoints sau.

## Coding Checklist (Pre-PR)
- UserResDTO là record, RegisterReqDTO là class
- Constructor injection 100%
- @Transactional tại register, updateMyProfile
- Idempotency-Key xử lý đúng flow
- Không trả Entity hoặc UserDetails qua API
- MDC correlationId xuất hiện trong logs khi debug
</task>
