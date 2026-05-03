<task>
# Module 12 – Cancellation & Refund

## Prerequisite
- Phase 0-11 hoàn tất, mvn clean compile pass.
- Booking module đã có trạng thái PENDING, CONFIRMED, CANCELLED.
- Payment module đã có PaymentStatus { PENDING, PAID, REFUNDED, FAILED }.
- Tuân thủ global_standards.md (Spring Boot 3.3.5, @SQLRestriction, ApiResponse, Redis busozy:${app.env}:..., MapStruct, Constructor Injection).
- Hiểu rõ luồng: Cancellation chỉ được phép khi booking ở trạng thái hợp lệ, refund amount tính theo policy thời gian.

## Database Tables
```sql
-- 1. Cancellation Policies (configurable refund rules)
CREATE TABLE cancellation_policies (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE, -- NULL = global policy
    route_id BIGINT REFERENCES routes(id) ON DELETE CASCADE,      -- NULL = áp dụng tất cả tuyến
    hours_before_departure INT NOT NULL,                          -- Ngưỡng thời gian (giờ)
    refund_percentage NUMERIC(5,2) NOT NULL CHECK (refund_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_cancellation_policies_scope ON cancellation_policies(company_id, route_id) WHERE deleted_at IS NULL AND is_active = TRUE;

-- 2. Cancellations (lịch sử hủy vé)
CREATE TABLE cancellations (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    cancelled_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    cancel_reason TEXT,
    cancel_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refund_amount NUMERIC(12,2) NOT NULL,
    refund_status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PROCESSED, FAILED, COMPLETED
    gateway_refund_id VARCHAR(100),               -- Mã hoàn tiền từ cổng thanh toán
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_refund_amount CHECK (refund_amount >= 0)
);
CREATE INDEX idx_cancellations_booking ON cancellations(booking_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cancellations_user ON cancellations(cancelled_by) WHERE deleted_at IS NULL;
CREATE INDEX idx_cancellations_refund_status ON cancellations(refund_status) WHERE deleted_at IS NULL;
```

## Entity

**`CancellationPolicy.java`** 
```java
@Entity @Table(name = "cancellation_policies") @SQLRestriction("deleted_at IS NULL")
public class CancellationPolicy extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "company_id") private Company company; // NULL = global
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "route_id") private Route route;       // NULL = all routes
    @Column(name = "hours_before_departure", nullable = false) private Integer hoursBeforeDeparture;
    @Column(name = "refund_percentage", nullable = false) private BigDecimal refundPercentage;
    @Column(name = "is_active") private Boolean isActive = true;
}
```
**`Cancellation.java`**
```java
@Entity @Table(name = "cancellations") @SQLRestriction("deleted_at IS NULL")
public class Cancellation extends BaseEntity {
    @OneToOne(fetch = LAZY) @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
    
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "cancelled_by", nullable = false)
    private User cancelledBy;
    
    @Column(columnDefinition = "TEXT") private String cancelReason;
    @Column(name = "cancel_time", nullable = false) private OffsetDateTime cancelTime;
    
    @Column(name = "refund_amount", nullable = false) private BigDecimal refundAmount;
    @Enumerated(EnumType.STRING) @Column(name = "refund_status")
    private RefundStatus refundStatus = RefundStatus.PENDING;
    
    @Column(name = "gateway_refund_id") private String gatewayRefundId;
}

public enum RefundStatus { PENDING, PROCESSED, FAILED, COMPLETED }
```
## DTOs
```
dto/request/
└── CancelBookingReqDTO.java
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CancelBookingReqDTO {
    @NotBlank @Size(max = 500) private String reason;
    private Boolean confirmRefundPolicy; // User phải confirm đã đọc policy
}
```
```
dto/response/
└── CancellationPreviewResDTO.java 
```
```java
public record CancellationPreviewResDTO(
    Long bookingId,
    String bookingCode,
    BigDecimal originalAmount,
    BigDecimal refundAmount,
    Double refundPercentage,
    String policyDescription,
    OffsetDateTime departureTime
) {}

public record CancellationResDTO(
    Long cancellationId,
    String bookingCode,
    BigDecimal refundAmount,
    RefundStatus refundStatus,
    String cancelReason,
    OffsetDateTime cancelTime,
    String cancelledByUsername
) {}
```
```
mapper/
└── CancellationMapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface CancellationMapper {
    CancellationResDTO toCancellationRes(Cancellation c);
    default String mapCancelledByUsername(User u) { return u != null ? u.getUsername() : null; }
}
```
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/bookings/{code}/cancellation-preview` | USER | Xem trước số tiền hoàn khi hủy |
| POST | `/api/v1/bookings/{code}/cancel` | USER | Hủy booking + khởi tạo hoàn tiền |
| GET | `/api/v1/bookings/{code}/cancellation` | USER | Chi tiết cancellation của booking |
| GET | `/api/v1/admin/cancellations` | ADMIN | Danh sách hủy vé (paginated, filter)  |
| POST | `/api/v1/admin/cancellations/{id}/process-refund` | ADMIN | Force xử lý hoàn tiền (nếu gateway fail) |
| GET | `/api/v1/admin/cancellation-policies` | ADMIN | Danh sách policy  |
| POST | `/api/v1/admin/cancellation-policies` | ADMIN | Tạo policy mới |
| PUT | `/api/v1/admin/cancellation-policies/{id}` | ADMIN | Cập nhật policy |

## Business Logic

### 1. Refund Policy Resolution (Ưu tiên áp dụng)
```java
public CancellationPolicy resolvePolicy(Booking booking) {
    // 1. Tìm policy cụ thể nhất: company + route
    CancellationPolicy policy = policyRepository
        .findByCompanyIdAndRouteIdAndActive(booking.getTrip().getRoute().getCompany().getId(), 
                                            booking.getTrip().getRoute().getId());
    if (policy != null) return policy;
    
    // 2. Fallback: company-only policy
    policy = policyRepository.findByCompanyIdAndRouteIdNullAndActive(booking.getTrip().getRoute().getCompany().getId());
    if (policy != null) return policy;
    
    // 3. Fallback: route-only policy (global route policy)
    policy = policyRepository.findByCompanyIdNullAndRouteIdAndActive(booking.getTrip().getRoute().getId());
    if (policy != null) return policy;
    
    // 4. Fallback: global default policy
    return policyRepository.findGlobalDefaultPolicy()
        .orElseGet(() -> new CancellationPolicy(null, null, 24, BigDecimal.valueOf(100), true)); // Default: 100% refund >24h
}
```

### 2. Calculate Refund Amount
```java
public BigDecimal calculateRefundAmount(Booking booking, CancellationPolicy policy) {
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    OffsetDateTime departure = booking.getTrip().getDepartureDate().atTime(booking.getTrip().getDepartureTime().toLocalTime())
        .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
    
    long hoursUntilDeparture = ChronoUnit.HOURS.between(now, departure);
    
    if (hoursUntilDeparture >= policy.getHoursBeforeDeparture()) {
        // Đủ điều kiện hoàn % theo policy
        return booking.getFinalPrice().multiply(policy.getRefundPercentage())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    } else {
        // Không hoàn tiền hoặc hoàn % thấp hơn (có thể có policy tiered)
        return BigDecimal.ZERO; // Hoặc implement tiered policy sau
    }
}
```

### 3. Cancellation Flow (User)
```java

@Transactional
public CancellationResDTO cancelBooking(Long userId, String bookingCode, CancelBookingReqDTO req) {
    // 1. Validate booking ownership & status
    Booking booking = validateBookingForCancellation(userId, bookingCode);
    
    // 2. Check if already cancelled
    if (booking.getStatus() == BookingStatus.CANCELLED) {
        throw new BusinessException(ErrorCode.CANCEL_002);
    }
    
    // 3. Resolve policy & calculate refund
    CancellationPolicy policy = resolvePolicy(booking);
    BigDecimal refundAmount = calculateRefundAmount(booking, policy);
    
    // 4. Update booking status
    booking.setStatus(BookingStatus.CANCELLED);
    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
    }
    bookingRepository.save(booking);
    
    // 5. Create cancellation record
    Cancellation cancellation = new Cancellation();
    cancellation.setBooking(booking);
    cancellation.setCancelledBy(userRepository.findById(userId).orElseThrow());
    cancellation.setCancelReason(req.getReason());
    cancellation.setCancelTime(OffsetDateTime.now());
    cancellation.setRefundAmount(refundAmount);
    cancellation.setRefundStatus(refundAmount.compareTo(BigDecimal.ZERO) > 0 ? RefundStatus.PENDING : RefundStatus.COMPLETED);
    cancellationRepository.save(cancellation);
    
    // 6. Process refund if needed (async via event)
    if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && booking.getPaymentStatus() == PaymentStatus.PAID) {
        eventPublisher.publishEvent(new RefundRequestedEvent(cancellation.getId(), booking.getPayment().getGatewayTransactionId(), refundAmount));
    }
    
    // 7. Invalidate caches
    cacheService.evictTripSeats(booking.getTrip().getId());
    cacheService.evictUserBookings(userId);
    
    return mapper.toCancellationRes(cancellation);
}
```

### 4. Refund Processing (Async Event Listener)
```java
@Component @RequiredArgsConstructor
public class RefundEventListener {
    private final PaymentGatewayProvider gatewayFactory;
    private final CancellationRepository cancellationRepository;
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundRequested(RefundRequestedEvent event) {
        Cancellation cancellation = cancellationRepository.findById(event.cancellationId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANCEL_001));
        
        try {
            // Call gateway refund API (MoMo/VNPAY)
            GatewayRefundResponse response = gatewayFactory.processRefund(
                cancellation.getBooking().getPayment().getGatewayTransactionId(),
                event.refundAmount(),
                "Refund for booking " + cancellation.getBooking().getBookingCode()
            );
            
            cancellation.setRefundStatus(RefundStatus.COMPLETED);
            cancellation.setGatewayRefundId(response.getTransactionId());
            cancellationRepository.save(cancellation);
            
        } catch (Exception e) {
            log.error("Refund failed for cancellation {}", event.cancellationId(), e);
            cancellation.setRefundStatus(RefundStatus.FAILED);
            cancellationRepository.save(cancellation);
            // Có thể thêm retry mechanism hoặc alert admin
        }
    }
}
```
## Error Codes
```java
CANCEL_001("CANCELLATION_NOT_FOUND", "Lịch sử hủy vé không tồn tại"),
CANCEL_002("BOOKING_ALREADY_CANCELLED", "Vé đã được hủy trước đó"),
CANCEL_003("CANCELLATION_NOT_ALLOWED", "Không thể hủy vé ở trạng thái này hoặc đã quá thời gian cho phép"),
CANCEL_004("REFUND_POLICY_NOT_FOUND", "Không tìm thấy chính sách hoàn tiền áp dụng"),
CANCEL_005("REFUND_PROCESSING_FAILED", "Xử lý hoàn tiền thất bại, vui lòng liên hệ admin"),
CANCEL_006("USER_NOT_AUTHORIZED", "Bạn không có quyền hủy vé này");
```
## Validation Rules
Field
Rule
Ghi chú
bookingCode
Phải thuộc user hiện tại, status IN (PENDING, CONFIRMED)
Service validation
reason
Max 500 chars, không chứa HTML/JS
Chống XSS
confirmRefundPolicy
Phải = true để submit
FE bắt buộc checkbox
refund_amount
>= 0 và <= booking.final_price
DB CHECK constraint
Policy resolution
Ưu tiên: company+route > company > route > global
Implement fallback chain
Timezone
Luôn dùng Asia/Ho_Chi_Minh cho tính giờ
Tránh sai lệch múi giờ

### Checklist Pre-PR
Cancellation và CancellationPolicy có @SQLRestriction, đúng constraints
Policy resolution logic implement fallback chain đúng thứ tự ưu tiên
Refund calculation dùng Asia/Ho_Chi_Minh timezone cho departure comparison
@TransactionalEventListener(phase = AFTER_COMMIT) cho refund async processing
Gateway refund integration dùng Strategy Pattern (reuse PaymentGatewayProvider)
Cache invalidation: trip:seats, user:bookings sau khi cancel
Admin endpoint process-refund có retry logic cho failed refunds
DTOs: Request=class + validation, Response=record
mvn clean compile + integration test cancellation flow pass
</task>
