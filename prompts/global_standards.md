<global_standards>

# Global Standards – Busozy Bus Booking System

## 1. Tech Stack
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.5 (LTS)
- **Database**: PostgreSQL 16 (JPA/Hibernate 6.4+ + Flyway)
- **Cache**: Redis 7+ (Spring Data Redis + Lettuce)
- **Build**: Maven
- **Libraries**: Lombok, MapStruct, Spring Validation, Hypersistence Utils (Hibernate 6.x compatible), Micrometer

## 2. Kiến Trúc & Cấu Trúc Thư Mục
### Layered Architecture (Bắt buộc)
`Controller → Service → Repository → Database`
- ❌ Không bỏ qua tầng. Controller không gọi trực tiếp Repository.
- ❌ Service không phụ thuộc `HttpServletRequest/Response`.

### Package Structure (`com.tomzxy.busozy`)

### Package Naming (groupId: `com.tomzxy.busozy`)
```
com.tomzxy.busozy
├── common/ # BaseEntity, ApiResponse, ErrorCode, enums, utils
├── config/ # Security, Redis, JPA, OpenAPI, WebMvc, FeatureFlags
├── exception/ # GlobalExceptionHandler, CustomExceptions
├── entity/ # JPA Entities (map 1:1 với DB)
├── dto/
│ ├── request/ # *ReqDTO (input)
│ └── response/ # *ResDTO (output)
├── mapper/ # MapStruct interfaces
├── repository/ # Spring Data JPA repositories
├── service/
│ ├── interfaces/ # Service contracts
│ └── impl/ # *ServiceImpl
└── controller/ # REST Controllers
```


### Separation of Concerns
| Layer        | Responsibility                                  |
|--------------|-------------------------------------------------|
| Controller   | Routing, `@Valid`, call Service, return `ApiResponse` |
| Service      | Business logic, transactions, external calls, cache |
| Repository   | Data access only. Complex queries via `@Query`/`Specification` |

## 3. Tiêu Chuẩn Coding (Java 21 + Spring Boot 3.3)
### Dependency Injection
- ✅ Dùng Constructor Injection: `@RequiredArgsConstructor` (Lombok).
- ❌ Tuyệt đối không dùng `@Autowired` field/method.

### DTO & Mapping
- ✅ Entity ↔ DTO: **Bắt buộc** dùng MapStruct.
- ✅ Output DTO: Java `record` (read-only, immutable).
- ✅ Input DTO: `class` với `@Data`/`@Builder` + Bean Validation.
- ❌ Không bao giờ trả Entity trực tiếp qua API.

### Null Safety & Optionals
- Repository find methods trả `Optional<T>`.
- Service không trả `null` → throw `ResourceNotFoundException` hoặc xử lý business logic.

### Validation
- `@Valid`/`@Validated` tại Controller.
- Dùng `@NotBlank`, `@Size`, `@Email`, `@Pattern` trong ReqDTO.
- Custom validators cho business rules (e.g., `@ValidPhone`, `@ValidSeatRange`).

## 4. Database & JPA
### Naming Conventions
| DB Table/Column | Java Class/Field |
|-----------------|------------------|
| `snake_case`    | `camelCase`      |
| `plural`        | `PascalCase`     |

### BaseEntity (Bắt buộc kế thừa)
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate @Column(updatable = false) private OffsetDateTime createdAt;
    @LastModifiedDate private OffsetDateTime updatedAt;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    
    public boolean isDeleted() { return deletedAt != null; }
}
```
- `@EnableJpaAuditing` phải được bật tại config class.

### Kiểu Thời Gian
- Dùng `OffsetDateTime` hoặc `Instant` cho các cột `TIMESTAMPTZ`.
- Dùng `LocalDate` cho cột `DATE`.

### Data Type Rules
- Money (price, fare, amount): BigDecimal (NUMERIC)
- Distance (distance_km, distance_from_origin): BigDecimal (NUMERIC)
- Latitude/Longitude: Double (NO precision/scale)

### Soft Delete Rules
- All tables with deleted_at must be filtered by default
- Use @Where(clause = "deleted_at IS NULL") OR enforce at repository layer
- Unique constraints must consider deleted_at (partial index if needed)

### JSONB
Dùng @Type(JsonBinaryType.class) (hypersistence-utils) → map sang Map<String,Object> hoặc POJO.
📌 Compatible với Spring Boot 3.3 + Hibernate 6.4+.

### Flyway
- Script migration đặt tại `src/main/resources/db/migration/`.
- Naming: `V1__init_schema.sql`, `V2__seed_data.sql`, ...

---

## 5. API & Error Handling

### Standard Response Wrapper
Mọi API **phải** trả về:
```json
{
  "success": true,
  "data": { ... },
  "message": "Thành công",
  "code": 200,
  "timestamp": "2026-04-19T12:45:00+07:00"
}
```
Implement qua `ApiResponse<T>` generic record:
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    int code,
    OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String message) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

### HTTP Status Codes
| Tình huống | Status |
|---|---|
| Thành công (GET/POST) | 200 OK |
| Tạo mới thành công | 201 Created |
| Không tìm thấy | 404 Not Found |
| Validation lỗi | 400 Bad Request |
| Chưa đăng nhập | 401 Unauthorized |
| Không có quyền | 403 Forbidden |
| Lỗi server | 500 Internal Server Error |
| Conflict (e.g. ghế đã đặt) | 409 Conflict |


### Idempotency & Versioning
🔹 Idempotency-Key: <uuid> bắt buộc cho: POST /bookings, /payments/initiate, /promotions/validate, /auth/register.
🔹 Store key → response trong Redis (TTL 24h).
🔹 API Versioning: /api/v1/.... Bump version khi có breaking change. Non-breaking dùng extension fields.

### Filtering/Sorting/Pagination
🔹 Pagination: page, size (max 100).
🔹 Sorting: sort=field,asc|desc.
🔹 Filtering: Dùng Specification<T> + RSQL parser cho dynamic queries. Tránh string concatenation.

### Exception Handling
- `@RestControllerAdvice` tại `GlobalExceptionHandler`.
- Custom exceptions: `ResourceNotFoundException`, `BusinessException`, `ConflictException`.
- Validation errors từ `@Valid` được bắt bởi `MethodArgumentNotValidException`.

---

## 6. Security (JWT – planned)
- JWT Access: 15m | Refresh: 7d.
- Refresh tokens lưu Redis (busozy:auth:refresh:{userId}).
- Spring Security Filter Chain: JWT filter → Auth manager → Authorization.
- Roles: ROLE_USER, ROLE_ADMIN, ROLE_COMPANY_OWNER.
- Password: BCryptPasswordEncoder. Salt rotation mỗi 2 năm.

---

## 7. Redis Usage Patterns
- **Key format**: `busozy:{domain}:{id}` (e.g. `busozy:trip_availability:123`).
- TTL: Bắt buộc. e.g. availability: 15m, seat_lock: 10m, refresh_token: 7d.
- Serialization: Jackson ObjectMapper (avoid default JDK serialization).
- Pattern: Cache-Aside + Write-Through cho critical paths.

---

## 8. Coding Checklist (trước khi submit code)
✅ Unit: JUnit 5 + Mockito + AssertJ. Coverage >80% service.
✅ Integration: Testcontainers (Postgres, Redis). 100% critical paths.
✅ Contract: Spring Cloud Contract hoặc OpenAPI generator sync backend↔frontend.
✅ Load: k6/Gatling cho /bookings, /payments (target: 500 RPS, P95 < 2s).
✅ Test Data: @Sql hoặc Testcontainers seed. Không dùng prod data dump.

## 9. Testing Standards
- Logging: SLF4J + Logback JSON. MDC với correlationId.
- Metrics: Micrometer + Prometheus. Expose /actuator/prometheus.
- Health: /actuator/health, /actuator/ready, /actuator/live.
- Tracing: Micrometer Tracing (OpenTelemetry compatible).
- Alerting: P95 > 2s, error rate > 1%, Redis memory > 80%, DB connections > 80%.
- CI/CD: Build → Test → SonarQube → Flyway dry-run → Deploy (Blue/Green).
- Config: Spring Cloud Config / K8s ConfigMap + Secrets.

## 10. Observability
- No direct Entity exposure
- All Entities extend BaseEntity
- ApiResponse wrapper applied
- @Valid + Bean Validation
- GlobalExceptionHandler catches all
- Constructor injection only
- MapStruct used
- @SQLRestriction for soft delete
- Idempotency key handled
- Correlation ID in logs
- Test coverage > threshold

## 11. API Design
- Idempotency: Header `Idempotency-Key` cho POST /bookings, /payments/initiate
- Pagination: Page-based (page, size) với max size = 100
- Sorting: `sort=field,dir` (dir: asc|desc)
- Filtering: RSQL/FiQL hoặc query params đơn giản



</global_standards>