<task>
# Module 03 – Company & Driver

## Prerequisite
- Module `00_infrastructure`, `01_auth`, `02_location` đã hoàn thành.
- Đọc `global_standards.md` trước khi bắt đầu.
- Tuân thủ: Spring Boot 3.3.5, `@SQLRestriction`, Redis key format `busozy:${app.env}:...`

## 1. Database & Migration
### Flyway Script: `V4__create_company_driver_tables.sql`
```sql
-- Companies (master data, soft delete)
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(20) UNIQUE,              -- Mã số thuế DN (optional)
    phone VARCHAR(15) NOT NULL,               -- Số điện thoại liên hệ
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
-- Partial index cho unique tax_code (chỉ xét active)
CREATE UNIQUE INDEX idx_companies_tax_code_active ON companies(tax_code) WHERE tax_code IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_companies_name ON companies USING gin(to_tsvector('vietnamese', name));

-- Drivers (soft delete, thuộc company)
CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    license_number VARCHAR(50) NOT NULL,      -- Số GPLX
    status VARCHAR(20) DEFAULT 'ACTIVE',      -- ACTIVE, INACTIVE, BLOCKED
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(company_id, license_number),       -- License unique per company
    UNIQUE(company_id, phone)                 -- Phone unique per company
);
-- Partial index cho soft delete
CREATE INDEX idx_drivers_company_active ON drivers(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_drivers_name ON drivers USING gin(to_tsvector('vietnamese', full_name));
```

## Entities Cần Tạo

**`Company`** (kế thừa `BaseEntity`):
```java
@Entity @Table(name = "companies") @SQLRestriction("deleted_at IS NULL")
public class Company extends BaseEntity {
    @Column(nullable = false) private String name;
    @Column(unique = true) private String taxCode;
    @Column(nullable = false) private String phone;
    private String address;
    @Column(name = "is_active") private Boolean isActive = true;
    
    @OneToMany(mappedBy = "company", fetch = LAZY)
    private List<Driver> drivers;
}
```
**`Driver + Reuse UserStatus enum`**:
```java
@Entity @Table(name = "drivers") @SQLRestriction("deleted_at IS NULL")
public class Driver extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false) private String fullName;
    @Column(nullable = false) private String phone;
    @Column(name = "license_number", nullable = false) private String licenseNumber;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;  // ✅ Reuse enum từ Phase 1
    
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String address;
}
```

## DTOs
```
dto/request/
├── CompanyReqDTO.java
└── DriverReqDTO.java  
```
```java
// CompanyReqDTO.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompanyReqDTO {
    @NotBlank @Size(min = 3, max = 100) private String name;
    @Pattern(regexp = "^[0-9]{10}(-[0-9]{3})?$", message = "Mã số thuế không hợp lệ")
    private String taxCode;
    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    @Size(max = 500) private String address;
}

// DriverReqDTO.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverReqDTO {
    @NotNull private Long companyId;
    @NotBlank @Size(min = 3, max = 100) private String fullName;
    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$") private String phone;
    @NotBlank @Pattern(regexp = "^[A-Z0-9]{6,20}$", message = "Số GPLX không hợp lệ")
    private String licenseNumber;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String address;
}
```
```
dto/response/
├── CompanyResDTO.java 
└── DriverResDTO.java  
```

```java
public record CompanyResDTO(
    Long id, String name, String taxCode, String phone, String address, 
    Boolean isActive, OffsetDateTime createdAt
) {}

public record DriverResDTO(
    Long id, 
    CompanySummary company,  // nested record
    String fullName, String phone, String licenseNumber, 
    UserStatus status, OffsetDateTime createdAt
) {
    public record CompanySummary(Long id, String name) {}
}
```
```
mapper/
└── CompanyDriver.java
```
```java
@Mapper(config = MapperConfig.class)
public interface CompanyDriverMapper {
    CompanyResDTO toCompanyRes(Company c);
    DriverResDTO toDriverRes(Driver d);
    
    // Custom mapping cho nested company
    default DriverResDTO.CompanySummary mapCompany(Company c) {
        return c != null ? new DriverResDTO.CompanySummary(c.getId(), c.getName()) : null;
    }
}
```
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/companies` | Public | Danh sách công ty active |
| GET | `/api/v1/companies/{id}` | Public | Chi tiết công ty |
| POST | `/api/v1/admin/companies` | ADMIN | Tạo công ty mới |
| PUT | `/api/v1/admin/companies/{id}` | ADMIN | Cập nhật công ty |
| DELETE | `/api/v1/admin/companies/{id}` | ADMIN | Soft delete công ty |
| GET | `/api/v1/admin/companies/{id}/drivers` | ADMIN | Danh sách tài xế của công ty |
| POST | `/api/v1/admin/drivers` | ADMIN | Thêm tài xế |
| PUT | `/api/v1/admin/drivers/{id}` | ADMIN | Cập nhật tài xế |
| PATCH | `/api/v1/admin/drivers/{id}/status` | ADMIN | Đổi trạng thái tài xế |

- Query Params cho GET /companies
    ?keyword=... → full-text search via to_tsvector('vietnamese', name)
    Pagination: page, size (max 100), sort: sort=name,asc

## Business Logic & Caching
- Redis Keys (theo chuẩn busozy:${app.env}:...)
1. busozy:${env}:company:list              → List<CompanyResDTO>, TTL 24h
2. busozy:${env}:company:{id}              → CompanyResDTO, TTL 4h
3. busozy:${env}:company:{id}:drivers      → List<DriverResDTO>, TTL 1h

- Cache Strategy
  Read: Cache-Aside pattern. Miss → query DB → cache → return.
  Write (Admin):
    Create/Update/Delete company → xóa company:list + company:{id}.
    Create/Update/Delete driver → xóa company:{companyId}:drivers.
  Validation:
   tax_code unique (partial index + service check).
   license_number + phone unique per company_id.
   Khi tạo driver: check companyId tồn tại + is_active == true.
- Critical Business Rule: Cannot Delete Company With Active Buses
```java
// Trong CompanyServiceImpl.java
@Transactional
public void deleteCompany(Long id) {
    Company company = companyRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
    
    // Check nếu company còn buses active (Phase 5+)
    if (busRepository.existsByCompanyIdAndIsActiveTrue(company.getId())) {
        throw new BusinessException(ErrorCode.COMPANY_HAS_ACTIVE_BUSES);
    }
    
    company.setDeletedAt(OffsetDateTime.now());
    company.setIsActive(false);
    companyRepository.save(company);
    
    // Invalidate cache
    evictCompanyCaches(company.getId());
}
```
### Error Codes (thêm vào ErrorCode.java)
```java
// Company errors
COMP_001("COMPANY_NOT_FOUND", "Công ty không tồn tại"),
COMP_002("TAX_CODE_EXISTS", "Mã số thuế đã được sử dụng"),
COMP_003("COMPANY_NOT_ACTIVE", "Công ty chưa được kích hoạt"),
COMP_004("COMPANY_HAS_ACTIVE_BUSES", "Không thể xóa công ty đang có xe hoạt động"),

// Driver errors
DRV_001("DRIVER_NOT_FOUND", "Tài xế không tồn tại"),
DRV_002("LICENSE_EXISTS_IN_COMPANY", "Số GPLX đã tồn tại trong công ty"),
DRV_003("PHONE_EXISTS_IN_COMPANY", "Số điện thoại đã tồn tại trong công ty"),
DRV_004("COMPANY_NOT_FOUND_FOR_DRIVER", "Công ty của tài xế không tồn tại");
```
### Validation Rules (VN-specific)
Field   | Regex | Detail

tax_code    | ^[0-9]{10}(-[0-9]{3})?$   | MST DN: 10 số hoặc 10-3 (chi nhánh)   |
phone   | `^0[3 | 5 |
license_number  | ^[A-Z0-9]{6,20}$  | GPLX: alphanumeric, 6-20 ký tự    |

### Checklist Pre-PR
- Tất cả entity kế thừa BaseEntity + @SQLRestriction
- DTOs: Request=class + validation, Response=record
- Redis key format đúng chuẩn busozy:${app.env}:...
- Cache invalidation khi admin CRUD
- Full-text search dùng to_tsvector('vietnamese', ...)
- Check company.hasActiveBuses trước khi soft delete
- mvn compile + mvn test pass

---

## 🗄️ Hướng Dẫn Tích Hợp Với Phase Trước

| Phase | Dữ liệu liên quan | Cách tích hợp |
|-------|------------------|---------------|
| **Phase 1 (Auth)** | `UserStatus` enum | ✅ Reuse trực tiếp cho `Driver.status` |
| **Phase 2 (Location)** | `province/district` | ⏳ Phase 4 (Route) sẽ link Company → Stops |
| **Phase 5 (Bus)** | `Bus.company_id` | ✅ Chuẩn bị FK `ON DELETE RESTRICT` để check trước khi xóa company |

---
</task>
