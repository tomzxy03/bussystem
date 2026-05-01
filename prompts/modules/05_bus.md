<task>
# Module 05 – Bus & Seat Layout (Xe & Sơ Đồ Ghế)

## Prerequisite
- Module `00_infrastructure`, `03_company_driver` đã hoàn thành.
- Đọc `global_standards.md` trước khi bắt đầu.
- Tuân thủ: Spring Boot 3.3.5, `@SQLRestriction`, Redis key format `busozy:${app.env}:...`, JSONB via Hypersistence Utils.

## 1. Database & Migration
### Flyway Script: `V6__create_bus_tables.sql`
```sql
-- Enable JSONB support (PostgreSQL native)
-- No extra extension needed, but ensure hypersistence-utils 3.9.0 is in pom.xml

-- Bus Types
CREATE TABLE bus_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,          -- e.g., "LIMOUSINE_9", "SLEEPER_40"
    name VARCHAR(100) NOT NULL,
    description TEXT,
    amenities JSONB,                           -- {"wifi": true, "ac": true, ...}
    base_price_per_km NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_bus_types_code_active ON bus_types(code) WHERE deleted_at IS NULL;

-- Seat Layouts
CREATE TABLE seat_layouts (
    id BIGSERIAL PRIMARY KEY,
    bus_type_id BIGINT NOT NULL REFERENCES bus_types(id) ON DELETE RESTRICT,
    name VARCHAR(100) UNIQUE NOT NULL,
    layout_data JSONB NOT NULL,                -- {"rows": 10, "cols": 4, "seats": [...]}
    total_seats INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_seat_layouts_type ON seat_layouts(bus_type_id) WHERE deleted_at IS NULL;

-- Buses
CREATE TABLE buses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    bus_type_id BIGINT NOT NULL REFERENCES bus_types(id) ON DELETE RESTRICT,
    seat_layout_id BIGINT REFERENCES seat_layouts(id) ON DELETE SET NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL, -- e.g., "51B-12345"
    bus_number VARCHAR(50),                    -- Internal fleet number
    name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',       -- ACTIVE, MAINTENANCE, INACTIVE
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_buses_plate_active ON buses(license_plate) WHERE deleted_at IS NULL;
CREATE INDEX idx_buses_company_active ON buses(company_id) WHERE deleted_at IS NULL;

-- Seats (Generated from layout or manually)
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,          -- e.g., "1A", "2B", "T1"
    seat_type VARCHAR(20) DEFAULT 'STANDARD',  -- STANDARD, VIP, SLEEPER, EXTRA
    row_num INT,
    col_num INT,
    price_multiplier NUMERIC(4,2) DEFAULT 1.00,-- e.g., 1.20 for VIP
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(bus_id, seat_number)
);
CREATE INDEX idx_seats_bus_active ON seats(bus_id) WHERE deleted_at IS NULL;
```

## Entities Cần Tạo

**`BusType`** 
```java
@Entity @Table(name = "bus_types") @SQLRestriction("deleted_at IS NULL")
public class BusType extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    private String description;
    @Type(JsonBinaryType.class) private Map<String, Object> amenities;
    private BigDecimal basePricePerKm = BigDecimal.ZERO;
    @Column(name = "is_active") private Boolean isActive = true;
}
```

**`SeatLayout`** 
```java
@Entity @Table(name = "seat_layouts") @SQLRestriction("deleted_at IS NULL")
public class SeatLayout extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "bus_type_id", nullable = false) private BusType busType;
    @Column(nullable = false, unique = true) private String name;
    @Type(JsonBinaryType.class) @Column(nullable = false) private Map<String, Object> layoutData;
    private Integer totalSeats;
}
```

**`Bus`**
```java
@Entity @Table(name = "buses") @SQLRestriction("deleted_at IS NULL")
public class Bus extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "bus_type_id", nullable = false) private BusType busType;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "seat_layout_id") private SeatLayout seatLayout;
    @Column(name = "license_plate", nullable = false, unique = true) private String licensePlate;
    private String busNumber, name;
    @Enumerated(EnumType.STRING) private BusStatus status = BusStatus.ACTIVE;
    @OneToMany(mappedBy = "bus", fetch = LAZY, cascade = ALL, orphanRemoval = true) private List<Seat> seats = new ArrayList<>();
}
```

**`Seat`** 
```java
@Entity @Table(name = "seats") @SQLRestriction("deleted_at IS NULL")
public class Seat extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "bus_id", nullable = false) private Bus bus;
    @Column(name = "seat_number", nullable = false) private String seatNumber;
    @Enumerated(EnumType.STRING) private SeatType seatType = SeatType.STANDARD;
    private Integer rowNum, colNum;
    private BigDecimal priceMultiplier = BigDecimal.ONE;
    @Column(name = "is_active") private Boolean isActive = true;
}
```
## Enums
```java
public enum BusStatus { ACTIVE, MAINTENANCE, INACTIVE }
public enum SeatType { STANDARD, VIP, SLEEPER, EXTRA, BED }
```

## DTOs
```
dto/request/
├── BusTypeReqDTO.java    
├── BusReqDTO.java        
└── SeatReqDTO.java   
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusTypeReqDTO {
    @NotBlank @Pattern(regexp = "^[A-Z0-9_]{3,30}$") private String code;
    @NotBlank @Size(max = 100) private String name;
    private String description;
    private Map<String, Object> amenities;
    @DecimalMin("0.0") private BigDecimal basePricePerKm = BigDecimal.ZERO;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusReqDTO {
    @NotNull private Long companyId;
    @NotNull private Long busTypeId;
    private Long seatLayoutId;
    @NotBlank @Pattern(regexp = "^[0-9]{1,2}[A-Z]{1,2}-[0-9]{4,5}$", message = "Biển số xe không hợp lệ")
    private String licensePlate;
    @Size(max = 50) private String busNumber, name;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatReqDTO {
    @NotBlank @Pattern(regexp = "^[A-Z0-9]{1,4}$") private String seatNumber;
    private SeatType seatType = SeatType.STANDARD;
    private Integer rowNum, colNum;
    @DecimalMin("0.1") @DecimalMax("5.0") private BigDecimal priceMultiplier = BigDecimal.ONE;
}
```
```
dto/response/
├── BusTypeResDTO.java    
├── SeatLayoutResDTO.java 
├── BusResDTO.java        
└── SeatResDTO.java       
```
```java
public record BusTypeResDTO(Long id, String code, String name, String description, 
                           Map<String, Object> amenities, BigDecimal basePricePerKm, Boolean isActive) {}

public record SeatLayoutResDTO(Long id, Long busTypeId, String name, Map<String, Object> layoutData, Integer totalSeats) {}

public record BusResDTO(Long id, String companyName, String busTypeCode, String busTypeName, 
                       String licensePlate, String busNumber, String name, BusStatus status, OffsetDateTime createdAt) {}

public record SeatResDTO(Long id, String seatNumber, SeatType seatType, Integer rowNum, Integer colNum, 
                        BigDecimal priceMultiplier, Boolean isActive) {}
```
```
mapper/
├── BusMapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface BusMapper {
    BusTypeResDTO toBusTypeRes(BusType bt);
    SeatLayoutResDTO toSeatLayoutRes(SeatLayout sl);
    BusResDTO toBusRes(Bus b);
    default String mapCompanyName(Company c) { return c != null ? c.getName() : null; }
    default String mapBusTypeName(BusType bt) { return bt != null ? bt.getName() : null; }
    SeatResDTO toSeatRes(Seat s);
}
```
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/bus-types` | Public | Danh sách loại xe active(cache 24h) |
| GET | `/api/v1/bus-types/{id}` | Public | Chi tiết loại xe |
| GET | `/api/v1/buses/{id}/seats` | Public | Layout ghế của xe (cho seat picker UI) |
| POST | `/api/v1/admin/bus-types` | ADMIN | Tạo loại xe mới |
| POST | `/api/v1/admin/seat-layouts` | ADMIN | Tạo seat layout |
| GET | `/api/v1/admin/buses` | ADMIN | Danh sách tất cả xe |
| POST | `/api/v1/admin/buses` | ADMIN | Thêm xe mới |
| PUT | `/api/v1/admin/buses/{id}` | ADMIN | Cập nhật xe |
| PATCH | `/api/v1/admin/buses/{id}/status` | ADMIN | Đổi trạng thái xe |
| POST | `/api/v1/admin/buses/{id}/seats` | ADMIN | Tạo/khởi tạo ghế cho xe |

## Business Logic & caching
1. Auto-Generate Seats from Layout
- Khi POST /admin/buses có seatLayoutId:
  Load layoutData JSONB.
  Validate cấu trúc: phải có seats array, mỗi item có row, col, type.
  bus.getSeats().clear() → bus.getSeats().addAll(mappedSeats).
  orphanRemoval = true sẽ xóa ghế cũ trong cùng @Transactional.
  Tính totalSeats từ layout, lưu vào Bus.seatLayout.totalSeats nếu chưa có.
2. Redis Caching (Format: busozy:${app.env}:bus:*)
  busozy:${env}:bus-types → List<BusTypeResDTO>, TTL 24h.
  busozy:${env}:bus:{id}:seats → List<SeatResDTO>, TTL 2h.
  Cache-Aside: Miss → query DB → cache → return.
  Invalidation: Admin CRUD bus/seat → xóa key tương ứng.
3. Validation Rules
  license_plate unique toàn hệ thống (partial index).
  seat_number unique trong cùng bus_id.
  price_multiplier trong khoảng 0.1 đến 5.0.
  Khi create bus: check companyId & busTypeId active.

## Error Codes (thêm vào ErrorCode.java)
```java
BUS_001("BUS_TYPE_NOT_FOUND", "Loại xe không tồn tại"),
BUS_002("BUS_TYPE_HAS_ACTIVE_BUSES", "Không thể xóa loại xe đang có xe hoạt động"),
BUS_003("LICENSE_PLATE_EXISTS", "Biển số xe đã tồn tại"),
BUS_004("INVALID_SEAT_LAYOUT", "Dữ liệu sơ đồ ghế không hợp lệ"),
BUS_005("BUS_NOT_FOUND", "Xe không tồn tại");

SEAT_001("SEAT_NUMBER_DUPLICATE", "Mã ghế trùng trong cùng xe"),
SEAT_002("SEAT_LAYOUT_MISMATCH", "Sơ đồ ghế không thuộc loại xe đã chọn"),
SEAT_003("INVALID_PRICE_MULTIPLIER", "Hệ số giá nằm ngoài khoảng 0.1 - 5.0");
```

## Checklist Pre-PR
- Tất cả entity kế thừa BaseEntity + @SQLRestriction
- JSONB dùng @Type(JsonBinaryType.class)
- DTOs: Request=class + validation, Response=record
- @Transactional tại create/update/replace methods
- Auto-generate seats atomic + clear old via orphanRemoval
- Redis key format đúng chuẩn busozy:${app.env}:...
- mvn compile + mvn test pass

## JSONB Format (amenities)
```json
{
  "wifi": true,
  "ac": true,
  "usb_charging": true,
  "blanket": false,
  "water": true
}
```

## JSONB Format (layout_data)
```json
{
  "rows": 10,
  "cols": 4,
  "seats": [
    {"row": 1, "col": 1, "type": "standard"},
    {"row": 1, "col": 2, "type": "vip"}
  ]
}
```
</task>
