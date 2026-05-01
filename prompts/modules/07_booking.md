<task>
# Module 07 – Booking (Đặt Vé & Giữ Ghế)

## Prerequisite
- Phase 0-6 hoàn tất, mvn clean compile pass.
- Đọc kỹ global_standards.md (Đặc biệt phần Transaction & Redis).
- Hiểu rõ logic tính giá theo pickup_order/dropoff_order và cơ chế Overlap Segment.
- Redis & PostgreSQL đang hoạt động ổn định để test luồng đặt vé.

1. Database & Migration
```sql
-- 1. Bảng Bookings (Giao dịch chính)
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_code UUID UNIQUE NOT NULL,              -- Mã tham chiếu công khai (ví dụ: BK-20260501-XXXX)
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE RESTRICT,
    
    -- Thông tin chặng đường
    pickup_order INT NOT NULL,
    dropoff_order INT NOT NULL,
    
    -- Giá tiền & Thanh toán
    base_price NUMERIC(12,2) NOT NULL,              -- Giá gốc của chặng
    final_price NUMERIC(12,2) NOT NULL,             -- Giá sau khi áp dụng multiplier ghế/promo
    currency VARCHAR(3) DEFAULT 'VND',
    payment_status VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, PAID, REFUNDED, FAILED
    
    -- Trạng thái & Thời gian
    status VARCHAR(20) DEFAULT 'PENDING',           -- PENDING, CONFIRMED, CANCELLED, EXPIRED, COMPLETED
    reserved_until TIMESTAMPTZ,                     -- Thời điểm giữ ghế hết hạn (cho trạng thái PENDING)
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_booking_orders CHECK (pickup_order < dropoff_order)
);

-- Index tối ưu cho truy vấn user & trip
CREATE INDEX idx_bookings_user ON bookings(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_trip_status ON bookings(trip_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_reserved ON bookings(status, reserved_until) WHERE status = 'PENDING' AND deleted_at IS NULL;

-- 2. Bảng Booking Seats (Chi tiết ghế đặt)
CREATE TABLE booking_seats (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(id) ON DELETE RESTRICT,
    final_price NUMERIC(10,2) NOT NULL,             -- Giá cụ thể cho ghế này (base * multiplier)
    currency VARCHAR(3) DEFAULT 'VND',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(booking_id, seat_id)
);
CREATE INDEX idx_booking_seats_seat ON booking_seats(seat_id) WHERE deleted_at IS NULL;

-- 3. Bảng Passengers (Thông tin hành khách)
CREATE TABLE passengers (
    id BIGSERIAL PRIMARY KEY,
    booking_seat_id BIGINT UNIQUE NOT NULL REFERENCES booking_seats(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    id_card VARCHAR(20),                            -- CMND/CCCD/Hộ chiếu
    date_of_birth DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
```

2. Entities

# Booking
```java
@Entity @Table(name = "bookings") @SQLRestriction("deleted_at IS NULL")
public class Booking extends BaseEntity {
    @Column(nullable = false, unique = true) private UUID bookingCode;
    
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    
    @Column(name = "pickup_order", nullable = false) private Integer pickupOrder;
    @Column(name = "dropoff_order", nullable = false) private Integer dropoffOrder;
    
    @Column(name = "base_price", nullable = false) private BigDecimal basePrice;
    @Column(name = "final_price", nullable = false) private BigDecimal finalPrice;
    @Column(nullable = false) private String currency = "VND";
    
    @Enumerated(EnumType.STRING) @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    
    @Column(name = "reserved_until") private OffsetDateTime reservedUntil;
    
    @OneToMany(mappedBy = "booking", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();
}
```
# Enums
- BookingStatus: PENDING (chờ thanh toán), CONFIRMED (đã thanh toán), CANCELLED (đã hủy), EXPIRED (hết hạn giữ ghế), COMPLETED (chuyến đã đi).
- PaymentStatus: PENDING, PAID, REFUNDED, FAILED.

# BookingSeat
```java
@Entity @Table(name = "booking_seats") @SQLRestriction("deleted_at IS NULL")
public class BookingSeat extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "seat_id", nullable = false) private Seat seat;
    @Column(name = "final_price", nullable = false) private BigDecimal finalPrice;
    @Column(nullable = false) private String currency = "VND";
    
    @OneToOne(mappedBy = "bookingSeat", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private Passenger passenger;
}
```
# Passenger
```java
@Entity @Table(name = "passengers") @SQLRestriction("deleted_at IS NULL")
public class Passenger extends BaseEntity {
    @OneToOne(fetch = LAZY) @JoinColumn(name = "booking_seat_id", nullable = false, unique = true)
    private BookingSeat bookingSeat;
    
    @Column(name = "full_name", nullable = false) private String fullName;
    @Column(nullable = false) private String phone;
    @Column(name = "id_card") private String idCard;
    private LocalDate dateOfBirth;
}
```
3. DTOs & Mapper
```
dto/request/
├── CreateBookingReqDTO.java
├── PassengerReqDTO.java
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateBookingReqDTO {
    @NotNull private Long tripId;
    @NotNull @Min(1) private Integer pickupOrder;
    @NotNull @Min(2) private Integer dropoffOrder;
    @NotNull @Size(min = 1, max = 10) private List<PassengerReqDTO> passengers; // Số lượng = số ghế
    private String promotionCode; // Optional
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PassengerReqDTO {
    @NotBlank @Size(max = 100) private String fullName;
    @NotBlank @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$") private String phone;
    @Size(max = 20) private String idCard;
    private LocalDate dateOfBirth;
    
    // Dùng để map sang Seat Entity
    @NotNull private Integer seatRow; 
    @NotNull private String seatCol;  
}
```
```
dto/response/
├── BookingResDTO.java
├── BookingDetailResDTO.java
```
```java
public record BookingResDTO(
    UUID bookingCode, Long tripId, String routeName, String companyName,
    LocalDate departureDate, OffsetTime departureTime,
    Integer pickupOrder, Integer dropoffOrder, 
    BigDecimal totalAmount, BookingStatus status, PaymentStatus paymentStatus,
    OffsetDateTime expiredAt
) {}

public record BookingDetailResDTO(
    BookingResDTO booking,
    List<BookingSeatResDTO> seats
) {
    public record BookingSeatResDTO(
        String seatNumber, String seatType, BigDecimal finalPrice, PassengerResDTO passenger
    ) {}
    public record PassengerResDTO(String fullName, String phone) {}
}
```
```
mapper/
├── BookingMapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface BookingMapper {
    BookingResDTO toBookingRes(Booking b);
    // Map nested relations
    default String mapRouteName(Trip t) { return t.getRoute() != null ? t.getRoute().getName() : null; }
    default String mapCompanyName(Trip t) { return t.getRoute() != null && t.getRoute().getCompany() != null ? t.getRoute().getCompany().getName() : null; }
    
    BookingDetailResDTO.BookingSeatResDTO toSeatRes(BookingSeat bs);
    BookingDetailResDTO.PassengerResDTO toPassengerRes(Passenger p);
}
```

4. API Endpoints
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/bookings` | USER | Tạo booking mới (reserve ghế) |
| GET | `/api/v1/bookings/my` | USER | Lịch sử đặt vé của user (paginated) |
| GET | `/api/v1/bookings/{code}` | USER | Chi tiết booking theo booking_code |
| POST | `/api/v1/bookings/{code}/cancel` | USER | Hủy booking |
| GET | `/api/v1/admin/bookings` | ADMIN | Danh sách tất cả bookings |

5. Business Logic & Risk Analysis ⚠️ (QUAN TRỌNG NHẤT)

 Đây là phần xử lý rủi ro "Overbooking" và đảm bảo tính nhất quán dữ liệu.
1. Flow Tạo Booking (POST /bookings)
- Bước 1: Validate & Idempotency
  Kiểm tra Idempotency-Key trong header. Nếu có trong Redis → Trả kết quả cũ.
  Validate trip tồn tại, status SCHEDULED, ngày đi >= hiện tại.
  Validate pickupOrder < dropoffOrder.
- Bước 2: Distributed Lock (Redis) - Chống Race Condition
  Dùng Redis Lock để đảm bảo tại một thời điểm chỉ có 1 request xử lý booking cho một chuyến xe cụ thể (hoặc ghế cụ thể).
  Key: busozy:{env}:lock:trip:{tripId}. TTL: 3 giây.
  Nếu không acquire được lock → Trả về lỗi ConflictException("Đang có người khác đặt vé cho chuyến này, vui lòng thử lại").
- Bước 3: Overlap Check (Source of Truth) - CHỐNG OVERBOOKING
  Chạy Native Query để kiểm tra xem các ghế định đặt có đang bị "chồng lấn" bởi các booking khác không.
  Logic Overlap: Một ghế được coi là KHÔNG THỂ đặt nếu có một booking khác đang giữ ghế đó trên đoạn đường trùng lặp.
  Công thức: NOT (b.dropoff_order <= :pickupOrder OR b.pickup_order >= :dropoffOrder)
   Nếu b.dropoff_order <= pickupOrder: Booking cũ kết thúc trước khi khách mới lên → OK.
   Nếu b.pickup_order >= dropoffOrder: Booking cũ bắt đầu sau khi khách mới xuống → OK.
   Ngược lại: Có sự chồng chéo.
- Query SQL:
```sql
// Trong BookingRepository
@Query(value = """
    SELECT bs.seat_id FROM booking_seats bs
    JOIN bookings b ON b.id = bs.booking_id
    WHERE b.trip_id = :tripId 
      AND bs.seat_id IN (:seatIds)
      AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'COMPLETED') -- Chỉ xét booking còn hiệu lực
      AND b.deleted_at IS NULL
      AND NOT (b.dropoff_order <= :pickupOrder OR b.pickup_order >= :dropoffOrder)
    """, nativeQuery = true)
List<Long> findOverlappingSeats(@Param("tripId") Long tripId, 
                                @Param("seatIds") List<Long> seatIds,
                                @Param("pickupOrder") Integer pickupOrder, 
                                @Param("dropoffOrder") Integer dropoffOrder);
```
  Nếu query trả về List có dữ liệu → Throw ConflictException("Ghế đã được đặt cho đoạn đường này").
- Bước 4: Tính Giá & Persist (Transaction)
  Lấy base_price từ route_prices theo (pickupOrder, dropoffOrder).
  Lấy multiplier từ seats. Tính final_price.
  Insert Booking (Status = PENDING, reservedUntil = NOW + 10 phút).
  Insert BookingSeat & Passenger.
  Commit Transaction.
- Bước 5: Post-Processing
  Xóa Redis Lock.
  Cache kết quả Idempotency.
  Invalidate Cache ghế của Trip (busozy:{env}:trip:seats:{tripId}).
2. Rủi ro & Cách giải quyết

Rủi ro  |  Mô tả    |   Giải pháp   |
Overbooking |   2 người cùng click đặt 1 ghế tại cùng 1ms. | 1. Redis Lock serialize request.
2. Query Overlap Check là bắt buộc. |
Ghế bị "giữ" mãi    |   User vào trang thanh toán nhưng bỏ ngang, ghế không ai đặt được.    |   Set reserved_until = NOW + 10 phút. Dùng @Scheduled job quét và đổi status sang EXPIRED sau 10p.    |
Giá vé bị sai   |   User thay đổi giá trên FE (inspect element).    |Backend tự tính toán giá dựa trên RoutePrice và SeatMultiplier từ DB, không bao giờ tin giá từ FE gửi lên. |
Dữ liệu không đồng bộ   |   Booking thành công nhưng Cache ghế vẫn hiện "Available".    |   Sau khi Insert Booking thành công, bắt buộc redisTemplate.delete(...).  |
Transaction Failure |   Lỗi mạng khi Insert BookingSeat sau khi Booking đã tạo. |   Dùng @Transactional. Nếu lỗi, toàn bộ rollback, không để lại Booking "rác".

6. Errors Codes
```java
BOOKING_001("BOOKING_NOT_FOUND", "Mã đặt vé không tồn tại"),
BOOKING_002("SEATS_FULL_OR_OVERLAP", "Ghế đã được đặt hoặc không khả dụng trên đoạn đường này"),
BOOKING_003("SEAT_LOCKED", "Ghế đang được giữ bởi người dùng khác, vui lòng thử lại sau"),
BOOKING_004("IDEMPOTENCY_DUPLICATE", "Yêu cầu đã được xử lý trước đó"),
BOOKING_005("INVALID_CANCEL_STATUS", "Không thể hủy vé ở trạng thái này hoặc quá giờ cho phép"),
BOOKING_006("TRIP_NOT_ACTIVE", "Chuyến xe không tồn tại hoặc đã hủy");
```

7. Validation Rules
Field   |   Rule    |   Ghi chú |
tripId  |   Tồn tại, status = SCHEDULED, departureDate >= TODAY |    Checked trong service   |   
pickupOrder, dropoffOrder   |   pickupOrder < dropoffOrder, có trong trip_segments  |   DB CHECK constraint + service validate  |   
passengers  |   Số lượng = số ghế. phone format VN. |   Bean Validation + loop check    | 
seatRow, seatCol    |   Map chính xác sang seat_number trong DB.    |   Tránh chọn ghế giả  |   
Idempotency-Key |   UUID v4 |   Header validation filter    |

8. Checklist Pre-PR
- Tất cả entity kế thừa BaseEntity + @SQLRestriction.
- Booking có @Version (nếu dùng optimistic lock) hoặc logic Redis Lock chặt chẽ.
- Query Overlap sử dụng đúng logic NOT (dropoff <= pickup OR pickup >= dropoff).
- reserved_until được set chính xác (ví dụ +10 phút) khi tạo PENDING.
- Scheduled Job xử lý booking EXPIRED hoạt động đúng.
- Cache trip:seats bị xóa mỗi khi tạo/hủy booking.
- DTOs: Request=class, Response=record, MapStruct mapping.
- mvn compile + mvn test pass.

</task>