<task>
# Module 06 – Trip & Availability (Chuyến Xe & Trạng Thái Ghế)

## Prerequisite
- Phase 0-5 hoàn tất, `mvn compile` pass.
- Tuân thủ `global_standards.md` (SB 3.3.5, @SQLRestriction, Redis key format, ApiResponse).

## 1. Database & Migration
`V7__create_trip_tables.sql`:
```sql
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE RESTRICT,
    bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE RESTRICT,
    driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    departure_date DATE NOT NULL,
    departure_time TIMETZ NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, SCHEDULED, DEPARTING, COMPLETED, CANCELLED
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, bus_id, departure_date, departure_time)
);
CREATE INDEX idx_trips_route_date ON trips(route_id, departure_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_bus_status ON trips(bus_id, status) WHERE deleted_at IS NULL;

CREATE TABLE trip_segments (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    pickup_order INT NOT NULL,
    dropoff_order INT NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(trip_id, pickup_order, dropoff_order),
    CHECK (pickup_order < dropoff_order)
);
CREATE INDEX idx_trip_segments_trip_avail ON trip_segments(trip_id) WHERE deleted_at IS NULL AND available_seats > 0;
```

## Entities Cần Tạo

**`Trip`**
```java
@Entity @Table(name = "trips") @SQLRestriction("deleted_at IS NULL")
public class Trip extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "route_id", nullable = false) private Route route;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "bus_id", nullable = false) private Bus bus;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "driver_id") private Driver driver;
    @Column(nullable = false) private LocalDate departureDate;
    @Column(nullable = false) private OffsetTime departureTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TripStatus status = TripStatus.DRAFT;
    @Version private Long version; // Optimistic lock
    @OneToMany(mappedBy = "trip", fetch = LAZY, cascade = ALL, orphanRemoval = true) private List<TripSegment> segments = new ArrayList<>();
}
```
**`TripSegment`** 
```java
@Entity @Table(name = "trip_segments") @SQLRestriction("deleted_at IS NULL")
public class TripSegment extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "trip_id", nullable = false) private Trip trip;
    @Column(name = "pickup_order", nullable = false) private Integer pickupOrder;
    @Column(name = "dropoff_order", nullable = false) private Integer dropoffOrder;
    @Column(name = "total_seats", nullable = false) private Integer totalSeats;
}
```

**`Enum`**
```java
public enum TripStatus { DRAFT, SCHEDULED, DEPARTED, COMPLETED, CANCELLED, DELAYED }
```
## DTOs
```
dto/request/
├── TripSearchReqDTO.java 
├── TripReqDTO.java      
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TripSearchReqDTO {
    @NotNull private Long originStopId;
    @NotNull private Long destStopId;
    @NotNull private LocalDate date;
    @Min(1) private Integer passengers = 1;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TripCreateReqDTO {
    @NotNull private Long routeId;
    @NotNull private Long busId;
    private Long driverId;
    @NotNull private LocalDate departureDate;
    @NotNull private OffsetTime departureTime;
}
```
```
dto/response/
├── TripResDTO.java 
├── TripDetailResDTO.java 
└── TripSegmentResDTO.java
```
```java
public record TripResDTO(
    Long id, String routeCode, String routeName, String companyName,
    String busLicensePlate, String busTypeName, String driverName,
    LocalDate departureDate, OffsetTime departureTime, TripStatus status
) {}

public record TripDetailResDTO(
    TripResDTO trip,
    List<TripSegmentResDTO> segments,
    BigDecimal minPrice, BigDecimal maxPrice
) {
    public record TripSegmentResDTO(Integer pickupOrder, Integer dropoffOrder, Integer totalSeats) {}
}
```
```
mapper/
├── TripMapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface TripMapper {
    TripResDTO toTripRes(Trip t);
    TripDetailResDTO.TripSegmentResDTO toSegmentRes(TripSegment s);
    default String mapRouteCode(Route r) { return r != null ? r.getCode() : null; }
    default String mapBusPlate(Bus b) { return b != null ? b.getLicensePlate() : null; }
}
```


## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/trips/search` | Public | Tìm chuyến (origin→dest, date) |
| GET | `/api/v1/trips/{id}` | Public | Detail chuyến + segment + price range |
| GET | `/api/v1/trips/{id}/seats` | Public | Sơ đồ ghế + trạng thái (DB truth) |
| POST | `/api/v1/admin/trips` | ADMIN | Tạo lịch chạy + auto init segments |
| PUT | `/api/v1/admin/trips/{id}` | ADMIN | Cập nhật chuyến |
| PATCH | `/api/v1/admin/trips/{id}/status` | ADMIN | Update status (DRAFT→SCHEDULED→...) |
| DELETE | `/api/v1/admin/trips/{id}` | ADMIN | Soft delete |

## Business Logic & Availability Strategy

### Auto Init Segments (POST /admin/trips)
- Validate: bus thuộc company của route, driver (nếu có) thuộc cùng company, không trùng lịch bus trong ngày.
- Load route_prices của route. Với mỗi (pickup_order, dropoff_order) → tạo 1 TripSegment.
- total_seats = bus.seats.size() (chỉ tính isActive=true).

### Availability Calculation (Source of Truth)
Không dùng trip_segments.available_seats. Tính động qua query:
```sql
SELECT s.id, s.seat_number,
  CASE WHEN bs.booking_id IS NOT NULL THEN 'BOOKED' ELSE 'AVAILABLE' END as status
FROM seats s
LEFT JOIN booking_seats bs ON s.id = bs.seat_id
  JOIN bookings b ON b.id = bs.booking_id
    AND b.trip_id = :tripId
    AND b.status NOT IN ('CANCELLED', 'EXPIRED')
    AND NOT (b.dropoff_order <= :pickupOrder OR b.pickup_order >= :dropoffOrder)
WHERE s.bus_id = :busId AND s.is_active = TRUE
```
-> Ghế không xuất hiện trong kết quả LEFT JOIN = AVAILABLE.

### Redis Cache Pattern
- Key: busozy:${app.env}:trip:seats:{tripId} → List<SeatStatusDTO>, TTL 15m.
- Cache-Aside: Miss → query DB → cache → return.
- Invalidation: Khi booking thành công/hủy → xóa key. Redis chỉ dùng để giảm tải DB, luôn re-check DB trước khi INSERT booking_seats.

### Validation Rules
- departure_date >= today.
- bus_id không được gán cho trip khác trong cùng ngày (check trips table).
- pickup_order < dropoff_order enforced ở DB CHECK constraint.
- Soft-delete trip chỉ cho phép khi status = DRAFT.

## Error Codes (thêm vào ErrorCode.java)
```java
TRIP_001("TRIP_NOT_FOUND", "Chuyến xe không tồn tại"),
TRIP_002("BUS_TIME_CONFLICT", "Xe đã được gán cho chuyến khác trong cùng khung giờ"),
TRIP_003("DRIVER_COMPANY_MISMATCH", "Tài xế không thuộc công ty quản lý chuyến"),
TRIP_004("ROUTE_STOP_NOT_FOUND", "Điểm đi/đến không thuộc tuyến đường"),
TRIP_005("CANNOT_DELETE_SCHEDULED_TRIP", "Chỉ được xóa chuyến ở trạng thái DRAFT"),
TRIP_006("SEGMENT_FULLY_BOOKED", "Phân đoạn ghế đã hết chỗ");
```

##  Checklist Pre-PR
- trip_segments auto-init đúng số lượng route_prices
- Availability query dùng NOT (dropoff_order <= pickup OR pickup_order >= dropoff)
- Redis cache 15m, invalidate on booking/cancel event
- @Transactional + @Version tại create/update status
- DB constraint CHECK (pickup_order < dropoff_order) hoạt động
- DTOs: Request=class, Response=record, MapStruct mapping
- mvn compile + mvn test pass
</task>
