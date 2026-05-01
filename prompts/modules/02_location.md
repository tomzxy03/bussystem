<task>
# Module 02 – Location (Provinces, Districts, Stops)

## Prerequisite
- Module `00_infrastructure` và `01_auth` đã hoàn thành, `mvn compile` pass.
- Đọc `global_standards.md` trước khi bắt đầu.
- Dataset tham khảo: https://github.com/thanglequoc/vietnamese-provinces-database

## 1. Database & Migration
### Flyway Script: `V3__create_location_tables.sql`
```sql
-- Enable earthdistance extension (optional, cho geo-search sau này)
CREATE EXTENSION IF NOT EXISTS earthdistance CASCADE;

-- Provinces (master data, hiếm khi thay đổi)
CREATE TABLE provinces (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,      -- Mã tỉnh từ dataset (e.g., "01", "79")
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),                   -- Tên tiếng Anh (optional)
    full_name VARCHAR(255),                 -- "Thành phố Hà Nội"
    full_name_en VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_provinces_code_active ON provinces(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_provinces_name ON provinces USING gin(to_tsvector('vietnamese', name));

-- Districts
CREATE TABLE districts (
    id BIGSERIAL PRIMARY KEY,
    province_id BIGINT NOT NULL REFERENCES provinces(id) ON DELETE RESTRICT,
    code VARCHAR(10) NOT NULL,              -- Mã quận từ dataset (e.g., "001", "760")
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    full_name VARCHAR(255),
    full_name_en VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(province_id, code)
);
CREATE INDEX idx_districts_province_code ON districts(province_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_districts_name ON districts USING gin(to_tsvector('vietnamese', name));

-- Stops (nghiệp vụ, có soft delete)
CREATE TABLE stops (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE,                -- Optional: mã nội bộ
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('STATION', 'OFFICE', 'ROAD_POINT', 'CUSTOM')),
    province_id BIGINT NOT NULL REFERENCES provinces(id) ON DELETE RESTRICT,
    district_id BIGINT NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    is_major BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
-- Partial index cho unique code (chỉ xét active)
CREATE UNIQUE INDEX idx_stops_code_active ON stops(code) WHERE code IS NOT NULL AND deleted_at IS NULL;
-- Geo index cho search theo khoảng cách (sau này dùng earthdistance)
CREATE INDEX idx_stops_location ON stops USING gist(ll_to_earth(latitude, longitude));
-- Full-text search cho tên + địa chỉ
CREATE INDEX idx_stops_search ON stops USING gin(to_tsvector('vietnamese', name || ' ' || COALESCE(address, '')));

```

## Entities Cần Tạo

**`Province`**
```java
@Entity @Table(name = "provinces") @SQLRestriction("deleted_at IS NULL")
public class Province extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    private String nameEn, fullName, fullNameEn;
    private Double latitude, longitude;
    @OneToMany(mappedBy = "province", fetch = LAZY) private List<District> districts;
}
```

**`District`**
```java
@Entity @Table(name = "districts") @SQLRestriction("deleted_at IS NULL")
public class District extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "province_id", nullable = false)
    private Province province;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String name;
    private String nameEn, fullName, fullNameEn;
    private Double latitude, longitude;
}
```

**`Stop + Enum StopType`** 
```java
public enum StopType { STATION, OFFICE, ROAD_POINT, CUSTOM }

@Entity @Table(name = "stops") @SQLRestriction("deleted_at IS NULL")
public class Stop extends BaseEntity {
    private String code;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StopType type;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "province_id", nullable = false) private Province province;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "district_id", nullable = false) private District district;
    private String address;
    @Column(nullable = false) private Double latitude, longitude;
    private Boolean isMajor = false, isActive = true;
}
```
## DTOs
```
dto/request/
├── StopReqDTO.java (class + validation)
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StopReqDTO {
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$") private String code;
    @NotBlank @Size(min = 3, max = 100) private String name;
    @NotNull private StopType type;
    @NotNull private Long provinceId;
    @NotNull private Long districtId;
    @Size(max = 500) private String address;
    @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
    private Boolean isMajor;
}
```

```
dto/response/
├── ProvinceResDTO.java 
├── DistrictResDTO.java
└── StopResDTO.java     
```

```java
public record ProvinceResDTO(Long id, String code, String name, String fullName, Double lat, Double lng) {}
public record DistrictResDTO(Long id, String code, String name, String fullName, Long provinceId) {}
public record StopResDTO(Long id, String code, String name, StopType type, 
                         String provinceName, String districtName, String address, 
                         Double lat, Double lng, Boolean isMajor) {}
```
## Mapper
```
mapper/
├── Locationapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface LocationMapper {
    ProvinceResDTO toProvinceRes(Province p);
    DistrictResDTO toDistrictRes(District d);
    StopResDTO toStopRes(Stop s);
    // Custom mapping cho nested fields
    default String mapProvinceName(Province p) { return p != null ? p.getName() : null; }
}
```
## API Endpoints
| Method | Path | Auth  | Query Params | Mô tả |
|---|---|---|---|
| GET | `/api/v1/provinces` | Public    | ?keyword=... | List provinces (cache Redis) |
| GET | `/api/v1/provinces/{id}/districts` | Public | - | Districts by province (cache) |
| GET | `/api/v1/stops` | Public    | ?provinceId=, ?districtId=, ?type=, ?isMajor=, ?keyword=, ?nearLat=, ?nearLng=, ?radiusKm= | Search stops |
| GET | `/api/v1/stops/{id}` | Public   | - | Stop detail |
| POST | `/api/v1/admin/stops` | ADMIN  | - | Create stop (idempotent) |
| PUT | `/api/v1/admin/stops/{id}` | ADMIN  | - | Update stop |
| DELETE | `/api/v1/admin/stops/{id}` | ADMIN   | - | Soft delete stop |

## Query Parameters cho GET /stops
- keyword: Full-text search via to_tsvector('vietnamese', ...).
- nearLat/nearLng/radiusKm: Dùng earthdistance (nếu enabled) hoặc Haversine formula trong @Query.
- Phân trang: page, size (max 100), sort: sort=name,asc.

## Business Logic & Caching
- Redis Keys (theo chuẩn busozy:${app.env}:location:...)
1. busozy:${env}:location:provinces           → List<ProvinceResDTO>, TTL 24h
2. busozy:${env}:location:districts:{provId}  → List<DistrictResDTO>, TTL 24h
3. busozy:${env}:location:stop:{id}           → StopResDTO, TTL 1h (vì có thể update)

- Cache Strategy
  Read: Cache-Aside pattern. Miss → query DB → cache → return.
  Write (Admin):
   Create/Update/Delete stop → xóa busozy:${env}:location:stop:{id}.
   Nếu thay đổi province/district → xóa key province/district list cache.
  Seed data: Khi deploy lần đầu, chạy script import từ vietnamese-provinces-database → cache warming.
- Validation Rules
  district.provinceId phải khớp với provinceId trong request.
  stops.code unique (chỉ xét active records, dùng partial index).
  Lat/lng trong range hợp lệ, address không vượt quá 500 ký tự.

## Error Codes (thêm vào ErrorCode.java)
1. LOC_001("PROVINCE_NOT_FOUND", "Tỉnh/thành phố không tồn tại"),
2. LOC_002("DISTRICT_NOT_FOUND", "Quận/huyện không tồn tại"),
3. LOC_003("DISTRICT_PROVINCE_MISMATCH", "Quận không thuộc tỉnh được chọn"),
4. LOC_004("STOP_CODE_EXISTS", "Mã điểm dừng đã tồn tại"),
5. LOC_005("INVALID_COORDINATES", "Tọa độ không hợp lệ");

## Checklist Pre-PR
Tất cả entity kế thừa BaseEntity + @SQLRestriction
DTOs: Request=class + validation, Response=record
Redis key format đúng chuẩn busozy:${env}:...
Cache invalidation khi admin CRUD
Full-text search dùng to_tsvector('vietnamese', ...)
Geo-search chuẩn bị earthdistance (optional)
mvn compile + mvn test pass


---

## 🗄️ Hướng Dẫn Import Dataset `vietnamese-provinces-database`

### Bước 1: Clone & chuẩn bị data
```bash
git clone https://github.com/thanglequoc/vietnamese-provinces-database.git
cd vietnamese-provinces-database/postgresql
```
### Bước 2: Tạo migration seed (V4__seed_locations.sql)
```sql
-- Import provinces (chỉ lấy fields cần thiết)
INSERT INTO provinces (code, name, name_en, full_name, full_name_en, latitude, longitude, created_at, updated_at)
SELECT 
    code, name, name_en, full_name, full_name_en, 
    latitude::DOUBLE PRECISION, longitude::DOUBLE PRECISION,
    NOW(), NOW()
FROM import_provinces; -- Bảng tạm import từ CSV

-- Import districts tương tự
INSERT INTO districts (province_id, code, name, name_en, full_name, full_name_en, latitude, longitude, created_at, updated_at)
SELECT 
    p.id, d.code, d.name, d.name_en, d.full_name, d.full_name_en,
    d.latitude::DOUBLE PRECISION, d.longitude::DOUBLE PRECISION,
    NOW(), NOW()
FROM import_districts d
JOIN provinces p ON p.code = d.province_code;
```
### Bước 3: Cache warming sau seed
```java
@Component
public class LocationCacheWarmer {
    @Autowired private RedisTemplate<String, Object> redis;
    @Autowired private ProvinceRepository provinceRepo;
    
    @PostConstruct
    @ConditionalOnProperty(name = "app.cache.warm-on-start", havingValue = "true")
    public void warmCache() {
        var provinces = provinceRepo.findAll().stream()
            .map(p -> new ProvinceResDTO(...)).toList();
        redis.opsForValue().set(
            "busozy:dev:location:provinces", 
            provinces, 
            Duration.ofHours(24)
        );
    }
}
```
</task>
