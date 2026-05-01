<task>
# Module 04 – Route & Pricing (Tuyến Đường & Bảng Giá)

## Prerequisite
- Module `00_infrastructure`, `01_auth`, `02_location`, `03_company_driver` đã hoàn thành.
- Đọc `global_standards.md` trước khi bắt đầu.
- ⚠️ CRITICAL: Tuân thủ tuyệt đối quy tắc pricing theo `Master_Plan.md`: **DÙNG `pickup_order` & `dropoff_order`, KHÔNG DÙNG `stop_id` để tính giá.**

## 1. Database & Migration
### Flyway Script: `V5__create_route_tables.sql`
```sql
-- Routes (thuộc Company, soft delete)
CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    code VARCHAR(50) UNIQUE NOT NULL,          -- e.g., "SG_HN_01"
    name VARCHAR(200) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL,
    duration_minutes INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_routes_code_active ON routes(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_routes_company_active ON routes(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_routes_name ON routes USING gin(to_tsvector('vietnamese', name));

-- Route Stops (sequence of stops in a route)
CREATE TABLE route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stops(id) ON DELETE RESTRICT,
    stop_order INT NOT NULL,                   -- 1, 2, 3... sequence
    estimated_minutes_from_origin INT NOT NULL,
    distance_from_origin DECIMAL(10,2) NOT NULL,
    is_pickup BOOLEAN DEFAULT TRUE,
    is_dropoff BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, stop_order),
    UNIQUE(route_id, stop_id)
);
CREATE INDEX idx_route_stops_route_order ON route_stops(route_id, stop_order) WHERE deleted_at IS NULL;

-- Route Prices (segment pricing based on ORDER, NOT stop_id)
CREATE TABLE route_prices (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    pickup_order INT NOT NULL,                 -- References route_stops.stop_order
    dropoff_order INT NOT NULL,                -- References route_stops.stop_order
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    currency VARCHAR(3) DEFAULT 'VND',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, pickup_order, dropoff_order),
    CONSTRAINT chk_price_orders CHECK (pickup_order < dropoff_order)
);
CREATE INDEX idx_route_prices_route_segment ON route_prices(route_id, pickup_order, dropoff_order) WHERE deleted_at IS NULL;
```

## Entities Cần Tạo

**`Route`**
```java
@Entity @Table(name = "routes") @SQLRestriction("deleted_at IS NULL")
public class Route extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    private BigDecimal distanceKm;
    private Integer durationMinutes;
    @Column(name = "is_active") private Boolean isActive = true;
    
    @OneToMany(mappedBy = "route", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private List<RouteStop> stops = new ArrayList<>();
    
    @OneToMany(mappedBy = "route", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private List<RoutePrice> prices = new ArrayList<>();
}
```

**`RouteStop & RoutePrice`** 
```java
@Entity @Table(name = "route_stops") @SQLRestriction("deleted_at IS NULL")
public class RouteStop extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "route_id", nullable = false) private Route route;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "stop_id", nullable = false) private Stop stop;
    @Column(name = "stop_order", nullable = false) private Integer stopOrder;
    private Integer estimatedMinutesFromOrigin;
    private BigDecimal distanceFromOrigin;
    private Boolean isPickup = true, isDropoff = true;
}

@Entity @Table(name = "route_prices") @SQLRestriction("deleted_at IS NULL")
public class RoutePrice extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "route_id", nullable = false) private Route route;
    @Column(name = "pickup_order", nullable = false) private Integer pickupOrder;
    @Column(name = "dropoff_order", nullable = false) private Integer dropoffOrder;
    @Column(nullable = false) private BigDecimal price;
    @Column(nullable = false) private String currency = "VND";
}
```

## DTOs
```
dto/request/
├── RouteReqDTO.java      
├── RouteStopReqDTO.java  
└── RoutePriceReqDTO.java
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RouteReqDTO {
    @NotBlank @Pattern(regexp = "^[A-Z0-9_]{3,50}$") private String code;
    @NotBlank @Size(max = 200) private String name;
    @DecimalMin("0.1") private BigDecimal distanceKm;
    @Min(1) private Integer durationMinutes;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RouteStopReqDTO {
    @NotNull private Long stopId;
    @NotNull @Min(1) private Integer stopOrder;
    @NotNull @Min(0) private Integer estimatedMinutesFromOrigin;
    @NotNull @DecimalMin("0.0") private BigDecimal distanceFromOrigin;
    private Boolean isPickup, isDropoff;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoutePriceReqDTO {
    @NotNull @Min(1) private Integer pickupOrder;
    @NotNull @Min(2) private Integer dropoffOrder;
    @NotNull @DecimalMin("1000") private BigDecimal price;
    private String currency = "VND";
}
```
```
dto/response/
├── RouteResDTO.java     
├── RouteDetailResDTO.java 
├── RouteStopResDTO.java  
└── RoutePriceResDTO.java 
```
```java
public record RouteResDTO(
    Long id, String code, String name, String companyName, 
    BigDecimal distanceKm, Integer durationMinutes, Boolean isActive, OffsetDateTime createdAt
) {}

public record RouteDetailResDTO(
    RouteResDTO route,
    List<RouteStopResDTO> stops,
    List<RoutePriceResDTO> prices
) {
    public record RouteStopResDTO(
        Integer stopOrder, String stopName, String provinceName, 
        Integer minutesFromOrigin, BigDecimal distanceFromOrigin, Boolean isPickup, Boolean isDropoff
    ) {}
    public record RoutePriceResDTO(
        Integer pickupOrder, Integer dropoffOrder, BigDecimal price, String currency
    ) {}
}
```

```
mapper/
├── RouteMapper.java    
```
```java
@Mapper(config = MapperConfig.class)
public interface RouteMapper {
    RouteResDTO toRouteRes(Route r);
    default String mapCompanyName(Company c) { return c != null ? c.getName() : null; }
    // Nested mapping cho stops/prices dùng @AfterMapping hoặc stream trong Service
}
```


## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/routes` | Public | Tìm kiếm tuyến (originStopId, destStopId) |
| GET | `/api/v1/routes/{id}` | Public | Chi tiết tuyến + danh sách stops |
| GET | `/api/v1/routes/{id}/prices` | Public | Bảng giá theo tuyến |
| POST | `/api/v1/admin/routes` | ADMIN | Tạo tuyến mới |
| PUT | `/api/v1/admin/routes/{id}` | ADMIN | Cập nhật tuyến |
| PUT | `/api/v1/admin/routes/{id}/stops` | ADMIN | Replace danh sách stops |
| PUT | `/api/v1/admin/routes/{id}/prices` | ADMIN | Replace bảng giá |
| DELETE | `/api/v1/admin/routes/{id}` | ADMIN | Soft delete tuyến |

## Query Logic GET /routes
- originStopId + destStopId: Join route_stops 2 lần, điều kiện origin.stop_order < dest.stop_order.
- Pagination: page, size (max 100), sort: sort=distanceKm,asc.

## Business Logic & Validation
1. Pricing Rule (BẮT BUỘC)
- pickup_order < dropoff_order enforced ở DB (CHECK constraint) + service validation.
- KHÔNG dùng from_stop_id/to_stop_id trong pricing. Dùng order để tránh N² combinations và dễ tính giá segment.
2. Replace-All Logic (Admin)
- Khi PUT /stops hoặc PUT /prices: Xóa toàn bộ old items (orphanRemoval = true), thêm mới trong cùng @Transactional.
- Validate stopOrder sequence liên tục (1..N) hoặc ít nhất tăng dần, không trùng.
- Validate origin & destination có trong danh sách stops và có isPickup/isDropoff = true.
3. Redis Caching
- Key: busozy:${env}:route:{id} → RouteDetailResDTO, TTL 2h.
- Cache-Aside: Miss → query DB + assemble DTO → cache → return.
- Invalidation: Admin update/replace/delete → xóa key.

## Error Codes (thêm vào ErrorCode.java)
```java
ROUTE_001("ROUTE_NOT_FOUND", "Tuyến đường không tồn tại"),
ROUTE_002("ROUTE_CODE_EXISTS", "Mã tuyến đã được sử dụng"),
ROUTE_003("INVALID_STOP_SEQUENCE", "Thứ tự điểm dừng không hợp lệ hoặc trùng lặp"),
ROUTE_004("ORIGIN_DEST_NOT_IN_ROUTE", "Điểm đi/đến không thuộc danh sách dừng của tuyến"),
ROUTE_005("INVALID_PRICE_SEGMENT", "pickup_order phải nhỏ hơn dropoff_order"),
ROUTE_006("COMPANY_NOT_ACTIVE", "Công ty chủ quản chưa được kích hoạt");
```

## Checklist Pre-PR
- Tất cả entity kế thừa BaseEntity + @SQLRestriction
- route_prices dùng pickup_order/dropoff_order (KHÔNG dùng stop_id)
- DTOs: Request=class + validation, Response=record
- @Transactional tại create/update/replace methods
- Cache invalidation đúng khi admin mutate
- DB constraint CHECK (pickup_order < dropoff_order) hoạt động
- mvn compile + mvn test pass
</task>
