<task>
# Module 09 – Promotion

## Prerequisite
- Phase 0-8 hoàn tất, mvn clean compile pass.
- Booking module đã sẵn sàng trạng thái PENDING + payment_status=PENDING.
- Tuân thủ global_standards.md (Spring Boot 3.3.5, @SQLRestriction, - ApiResponse, Redis busozy:${app.env}:..., MapStruct, Constructor Injection).
- Hiểu rõ flow: Promotion được validate khi user chọn mã → discount áp dụng vào final_price của Booking → used_count tăng khi payment thành công.

## Database Tables
```sql
-- 1. Promotions (master data, soft delete)
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,           -- Mã khuyến mãi (e.g., "SUMMER2024")
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Discount config
    discount_type VARCHAR(20) NOT NULL,         -- PERCENTAGE, FIXED
    discount_value NUMERIC(10,2) NOT NULL,      -- 10.00 (10%) hoặc 50000 (50k VND)
    min_order_value NUMERIC(12,2) DEFAULT 0,    -- Đơn tối thiểu để áp dụng
    max_discount NUMERIC(12,2),                 -- Giới hạn giảm tối đa (cho PERCENTAGE)
    
    -- Validity & usage
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    usage_limit INT,                            -- NULL = không giới hạn
    used_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,               -- Số lần tối đa mỗi user được dùng
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    CONSTRAINT chk_discount_value CHECK (discount_value >= 0),
    CONSTRAINT chk_validity CHECK (valid_to >= valid_from)
);
CREATE UNIQUE INDEX idx_promotions_code_active ON promotions(code) WHERE deleted_at IS NULL AND is_active = TRUE;
CREATE INDEX idx_promotions_validity ON promotions(valid_from, valid_to) WHERE deleted_at IS NULL AND is_active = TRUE;

-- 2. Promotion Routes (M2M: promotion áp dụng cho những tuyến nào)
CREATE TABLE promotion_routes (
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (promotion_id, route_id)
);
CREATE INDEX idx_promotion_routes_route ON promotion_routes(route_id);

-- 3. User Promotion Usage (track per-user usage limit)
CREATE TABLE user_promotion_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    used_count INT DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    UNIQUE(user_id, promotion_id)
);
CREATE INDEX idx_user_promotion_usage_user ON user_promotion_usage(user_id);
```

## Entities

**`Promotion`** 
```java
@Entity @Table(name = "promotions") @SQLRestriction("deleted_at IS NULL")
public class Promotion extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    private String description;
    
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;
    
    @Column(name = "discount_value", nullable = false) private BigDecimal discountValue;
    @Column(name = "min_order_value") private BigDecimal minOrderValue = BigDecimal.ZERO;
    @Column(name = "max_discount") private BigDecimal maxDiscount;
    
    @Column(name = "valid_from", nullable = false) private OffsetDateTime validFrom;
    @Column(name = "valid_to", nullable = false) private OffsetDateTime validTo;
    
    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "used_count") private Integer usedCount = 0;
    @Column(name = "per_user_limit") private Integer perUserLimit = 1;
    
    @Column(name = "is_active") private Boolean isActive = true;
    
    @ManyToMany(fetch = LAZY)
    @JoinTable(name = "promotion_routes",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "route_id"))
    private Set<Route> applicableRoutes = new HashSet<>();
}

public enum DiscountType { PERCENTAGE, FIXED }
```
**`UserPromotionUsage`**
```java
@Entity @Table(name = "user_promotion_usage")
public class UserPromotionUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "promotion_id", nullable = false) private Promotion promotion;
    
    @Column(name = "used_count") private Integer usedCount = 0;
    @Column(name = "last_used_at") private OffsetDateTime lastUsedAt;
}
```

## DTOs
```
dto/request/
├── PromotionReqDTO.java         
└── PromotionValidateReqDTO.java 
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionValidateReqDTO {
    @NotBlank private String promotionCode;
    @NotNull private Long routeId;
    @NotNull @DecimalMin("0.01") private BigDecimal orderValue;
    @NotNull private Long userId;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionReqDTO {
    @NotBlank @Pattern(regexp = "^[A-Z0-9_-]{3,50}$") private String code;
    @NotBlank @Size(max = 100) private String name;
    private String description;
    @NotNull private DiscountType discountType;
    @NotNull @DecimalMin("0.0") private BigDecimal discountValue;
    @DecimalMin("0.0") private BigDecimal minOrderValue = BigDecimal.ZERO;
    @DecimalMin("0.0") private BigDecimal maxDiscount;
    @NotNull private OffsetDateTime validFrom;
    @NotNull private OffsetDateTime validTo;
    @Min(1) private Integer usageLimit;
    @Min(1) private Integer perUserLimit = 1;
    private List<Long> routeIds;
}
```
```
dto/response/
├── PromotionResDTO.java         
└── PromotionValidateResDTO.java 
```
```java
public record PromotionValidateResDTO(
    boolean isValid,
    String promotionCode,
    String promotionName,
    BigDecimal discountAmount,
    BigDecimal finalPrice,
    String message
) {}

public record PromotionResDTO(
    Long id, String code, String name, String description,
    DiscountType discountType, BigDecimal discountValue,
    BigDecimal minOrderValue, BigDecimal maxDiscount,
    OffsetDateTime validFrom, OffsetDateTime validTo,
    Integer usageLimit, Integer usedCount, Integer perUserLimit,
    Boolean isActive, OffsetDateTime createdAt
) {}
```
```
mapper/
├── PromotionMapper.java 
```
```java
@Mapper(config = MapperConfig.class)
public interface PromotionMapper {
    PromotionResDTO toPromotionRes(Promotion p);
    PromotionValidateResDTO toValidateRes(boolean isValid, Promotion p, BigDecimal discountAmount, BigDecimal finalPrice, String message);
}
```
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/promotions/validate` | USER | Kiểm tra & tính discount |
| GET | `/api/v1/admin/promotions` | ADMIN | Danh sách tất cả mã |
| GET | `/api/v1/admin/promotions/{id}` | ADMIN | Chi tiết mã |
| POST | `/api/v1/admin/promotions` | ADMIN | Tạo mã mới |
| PUT | `/api/v1/admin/promotions/{id}` | ADMIN | Cập nhật mã |
| DELETE | `/api/v1/admin/promotions/{id}` | ADMIN | Soft delete |

## Business Logic – Validate Promotion
# Validation Promotion Flow
```java
public PromotionValidateResDTO validate(PromotionValidateReqDTO req) {
    // 1. Tìm promotion theo code (active, not deleted, not expired)
    Promotion promo = promotionRepository.findByCodeAndActive(req.getPromotionCode())
        .orElseThrow(() -> new BusinessException(ErrorCode.PROMO_001));
    
    // 2. Check validity window
    OffsetDateTime now = OffsetDateTime.now();
    if (now.isBefore(promo.getValidFrom()) || now.isAfter(promo.getValidTo())) {
        return PromotionValidateResDTO(false, null, null, null, null, "Mã không còn hiệu lực");
    }
    
    // 3. Check usage limit (global)
    if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
        return PromotionValidateResDTO(false, null, null, null, null, "Mã đã hết lượt sử dụng");
    }
    
    // 4. Check per-user limit
    if (promo.getPerUserLimit() != null) {
        int userUsed = userPromotionUsageRepository.countByUserIdAndPromotionId(req.getUserId(), promo.getId());
        if (userUsed >= promo.getPerUserLimit()) {
            return PromotionValidateResDTO(false, null, null, null, null, "Bạn đã dùng mã này tối đa số lần cho phép");
        }
    }
    
    // 5. Check min_order_value
    if (req.getOrderValue().compareTo(promo.getMinOrderValue()) < 0) {
        return PromotionValidateResDTO(false, null, null, null, null, 
            "Đơn hàng tối thiểu " + promo.getMinOrderValue() + " VND để áp dụng mã");
    }
    
    // 6. Check route restriction (nếu có)
    if (!promo.getApplicableRoutes().isEmpty()) {
        boolean routeMatch = promo.getApplicableRoutes().stream()
            .anyMatch(r -> r.getId().equals(req.getRouteId()));
        if (!routeMatch) {
            return PromotionValidateResDTO(false, null, null, null, null, "Mã không áp dụng cho tuyến này");
        }
    }
    
    // 7. Calculate discount
    BigDecimal discountAmount = calculateDiscount(promo, req.getOrderValue());
    BigDecimal finalPrice = req.getOrderValue().subtract(discountAmount);
    
    return PromotionValidateResDTO(true, promo.getCode(), promo.getName(), 
        discountAmount, finalPrice, "Áp dụng thành công");
}

private BigDecimal calculateDiscount(Promotion promo, BigDecimal orderValue) {
    BigDecimal discount;
    if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
        discount = orderValue.multiply(promo.getDiscountValue().divide(BigDecimal.valueOf(100)));
        if (promo.getMaxDiscount() != null) {
            discount = discount.min(promo.getMaxDiscount());
        }
    } else { // FIXED
        discount = promo.getDiscountValue().min(orderValue); // Không giảm quá giá trị đơn
    }
    return discount.setScale(2, RoundingMode.HALF_UP);
}
```
# Apply Promotion khi Confirm Booking (Phase 7 Integration)
- Không tăng used_count khi validate → chỉ tăng khi payment thành công.
- Dùng optimistic update để tránh race condition:
```java
@Transactional
public boolean incrementUsage(Long promotionId) {
    int updated = promotionRepository.incrementUsedCount(promotionId);
    return updated > 0; // true if row was updated (usage_limit not exceeded)
}

// Trong BookingService.confirmPayment():
if (booking.getPromotionId() != null) {
    boolean success = promotionService.incrementUsage(booking.getPromotionId());
    if (!success) {
        // Handle edge case: promotion reached limit between validate and payment
        log.warn("Promotion usage limit reached after validation: id={}", booking.getPromotionId());
    }
    // Also update user_promotion_usage
    userPromotionUsageService.incrementUserUsage(booking.getUserId(), booking.getPromotionId());
}
```
# Cache Stratege
- Validate endpoint: Không cache (luôn query DB để đảm bảo freshness của used_count, validity).
- GET /promotions/active: Cache list mã active theo route, TTL 15 phút.
```
Key: busozy:${env}:promotion:active:route:{routeId}
TTL: 15m
Invalidate: Khi admin tạo/sửa/xóa promotion
```

## Error Codes
```java
PROMO_001("PROMOTION_NOT_FOUND", "Mã khuyến mãi không tồn tại hoặc không hoạt động"),
PROMO_002("PROMOTION_EXPIRED", "Mã khuyến mãi đã hết hạn"),
PROMO_003("PROMOTION_USAGE_EXCEEDED", "Mã đã hết lượt sử dụng"),
PROMO_004("PROMOTION_USER_LIMIT_EXCEEDED", "Bạn đã dùng mã này tối đa số lần cho phép"),
PROMO_005("PROMOTION_MIN_ORDER_NOT_MET", "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã"),
PROMO_006("PROMOTION_ROUTE_NOT_APPLICABLE", "Mã không áp dụng cho tuyến đường này"),
PROMO_007("PROMOTION_ALREADY_APPLIED", "Mã đã được áp dụng cho đơn hàng này");
```

## Apply Promotion (khi confirm booking)
- `used_count` tăng 1 khi booking được **confirmed** (payment success), không phải khi validate.
- Dùng `UPDATE promotions SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit` để tránh race condition.

## Validation
Field   |   Rule    |   Ghi chú |
code    |   ^[A-Z0-9_-]{3,50}$, unique, case-insensitive    |   Index partial cho active codes  |
discountValue   |   >= 0, PERCENTAGE: 0-100, FIXED: any positive    |   Validate trong service  |   
validTo |   >= validFrom    |   DB CHECK constraint + service validate  |
usageLimit  |   >= usedCount khi update |   Optimistic update pattern   |
routeIds    |   |   Tất cả route phải tồn tại & active  |   Validate trước khi save |
orderValue trong validate   |   > 0 |   Server-side check   |

## Checklist Pre-PR
- Tất cả entity kế thừa BaseEntity + @SQLRestriction (trừ UserPromotionUsage)
- Promotion có @ManyToMany với Route qua promotion_routes
- DTOs: Request=class + validation, Response=record
- @Transactional tại incrementUsage() với optimistic update
- Cache promotion:active:route:{id} invalidate khi admin mutate
- Validate endpoint không cache, luôn query DB freshness
- Integration với Booking: used_count chỉ tăng khi payment success
- mvn compile + mvn test pass

## Ghi Chú Triển Khai Thực Tế
Timezone handling: valid_from/valid_to dùng OffsetDateTime, so sánh với OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")) để đúng múi giờ VN.
Case-insensitive code lookup: Index LOWER(code) hoặc dùng ILIKE trong query.
Admin UI: Khi tạo promotion, FE nên show preview discount calculation cho các order value mẫu.
Analytics: Phase 12 Admin có thể thêm report: "Top promotions by usage", "Revenue impact".
</task>
