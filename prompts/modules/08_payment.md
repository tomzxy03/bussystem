<task>
# Module 08 – Payment

## Prerequisite
- Phase 0-7 hoàn tất, mvn clean compile pass.
- Booking module đã sẵn sàng trạng thái PENDING + payment_status=PENDING.
- Tuân thủ global_standards.md (Spring Boot 3.3.5, @SQLRestriction, ApiResponse, Redis busozy:${app.env}:..., MapStruct, Constructor Injection).
- Lưu ý triển khai: VNPAY/MoMo thực tế có thể tích hợp sau. Phase này sẽ dùng MockGateway (trả URL giả lập) để FE test luồng chuyển khoản. Khi có tài khoản merchant, chỉ cần implement interface PaymentGatewayProvider là xong.

## Database Tables
```sql
-- 1. Payment Methods (Cấu hình phương thức thanh toán)
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,      -- COD, BANK_TRANSFER, MOMO, VNPAY
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    fee_percentage NUMERIC(4,2) DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Payments (Bản ghi giao dịch chính)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    payment_method_id BIGINT NOT NULL REFERENCES payment_methods(id) ON DELETE RESTRICT,
    gateway_transaction_id VARCHAR(100),   -- Mã giao dịch từ cổng (MoMo/VNPAY)
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PAID, FAILED, REFUNDED, CANCELLED
    gateway_response JSONB,                -- Lưu raw callback để audit/debug
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(gateway_transaction_id)         -- Idempotency cấp DB
);
CREATE INDEX idx_payments_booking ON payments(booking_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payments_status ON payments(status) WHERE deleted_at IS NULL;

-- Seed data phương thức thanh toán
INSERT INTO payment_methods (code, name, is_active) VALUES 
('COD', 'Thanh toán tại quầy', true),
('BANK_TRANSFER', 'Chuyển khoản ngân hàng', true);
-- (MOMO, VNPAY sẽ bật is_active=true sau khi tích hợp)
```
## Entities

**`PaymentMethod`**
```java
@Entity @Table(name = "payment_methods")
public class PaymentMethod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "is_active") private Boolean isActive = true;
    private BigDecimal feePercentage = BigDecimal.ZERO;
    @CreatedDate private OffsetDateTime createdAt;
    @LastModifiedDate private OffsetDateTime updatedAt;
}
```

**`Payment`** 
```java
@Entity @Table(name = "payments") @SQLRestriction("deleted_at IS NULL")
public class Payment extends BaseEntity {
    @OneToOne(fetch = LAZY) @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
    
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(name = "gateway_transaction_id", unique = true) private String gatewayTransactionId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false) private String currency = "VND";
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Type(JsonBinaryType.class) @Column(name = "gateway_response")
    private Map<String, Object> gatewayResponse;
}

public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED, CANCELLED }
```
## DTOs
```
dto/request/
└── PaymentInitiateReqDTO.java 
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentInitiateReqDTO {
    @NotNull private Long bookingId;
    @NotBlank private String methodCode; // COD, BANK_TRANSFER
}
```
```
dto/response/
├── PaymentInitiateResDTO.java 
└── PaymentResDTO.java 
```
```java
public record PaymentInitiateResDTO(
    Long paymentId, 
    String bookingCode, 
    BigDecimal amount,
    String paymentUrl,      // URL chuyển khoản (null nếu COD)
    String qrCodeData,      // QR string (null nếu COD)
    String directPayUrl     // Deep link app ngân hàng (null nếu COD)
) {}

public record PaymentStatusResDTO(
    Long paymentId, String bookingCode, String methodCode, 
    BigDecimal amount, PaymentStatus status, String gatewayTransactionId
) {}
```
```
mapper/
├── PaymentMapper.java 
```
```java
@Mapper(config = MapperConfig.class)
public interface PaymentMapper {
    PaymentStatusResDTO toStatusRes(Payment p);
    default String mapMethodCode(PaymentMethod pm) { return pm != null ? pm.getCode() : null; }
}
```
## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/payment-methods` | Public | Danh sách phương thức thanh toán |
| POST | `/api/v1/payments/initiate` | USER | Khởi tạo thanh toán (COD,Online) |
| POST | `/api/v1/payments/callback/{provider}` | Public | Webhook nhận kết quả từ cổng |
| POST | `/api/v1/payments/{id}/cancel` | USER | Hủy thanh toán pending (chưa quá 10p) |
| GET | `/api/v1/payments/{id}` | USER | Kiểm tra trạng thái |

## Business Logic

### Strategy Pattern Cho Gateway (Future-Proof)
# Thiết kế interface để dễ dàng thêm VNPAY/MoMo sau mà không động vào PaymentService.
```java
public interface PaymentGatewayProvider {
    boolean supports(String methodCode);
    GatewayInitiateResponse initiate(Payment payment, String returnUrl);
    boolean verifySignature(Map<String, String> params);
    PaymentProcessResult processCallback(Map<String, String> params);
}

// Hiện tại dùng Mock
@Component @Primary
public class MockGatewayProvider implements PaymentGatewayProvider {
    public boolean supports(String code) { return "BANK_TRANSFER".equals(code); }
    public GatewayInitiateResponse initiate(Payment p, String returnUrl) {
        // Trả URL sandbox giả lập: https://sandbox.bank/pay?ref={paymentId}
        return new GatewayInitiateResponse("https://mock-pay.local/" + p.getId(), null, null);
    }
    // Verify & Process callback mock...
}
```

### Flow Khởi Tạo (POST /initiate)
1. Validate bookingId tồn tại, status=PENDING, payment_status=PENDING, reserved_until > NOW.
2. Validate methodCode active.
3. Check amount: payment.amount phải == booking.final_price.
4. Tạo record Payment (status=PENDING).
5. Xử lý theo phương thức:
  COD: Update Booking.paymentStatus=PAID, Booking.status=CONFIRMED. Trả paymentUrl=null.
  BANK_TRANSFER: Gọi PaymentGatewayProvider.initiate() → Lấy paymentUrl. Trả cho FE redirect.
6. Invalidate cache ghế trip:seats:{tripId} (chuyển từ giữ ghế sang đặt thành công).

### Webhook Handling (POST /callback/{provider})
# Đây là điểm dễ bị tấn công/retry storm. Xử lý cực kỳ chặt chẽ:
```java
@Transactional
public void handleWebhook(String provider, Map<String, String> params) {
    // 1. Verify Signature (HMAC-SHA256) + IP Whitelist (nếu có)
    PaymentGatewayProvider gateway = gatewayFactory.getProvider(provider);
    if (!gateway.verifySignature(params)) throw new BusinessException(ErrorCode.PAY_003);

    // 2. Idempotency Check: Tìm payment theo gateway_transaction_id
    Payment payment = paymentRepository.findByGatewayTransactionId(params.get("transactionId"));
    if (payment != null && payment.getStatus() != PaymentStatus.PENDING) {
        return; // Đã xử lý rồi → trả 200 OK ngay để cổng không retry
    }

    // 3. Process Callback
    PaymentProcessResult result = gateway.processCallback(params);
    
    // 4. Update Payment & Booking
    payment.setStatus(result.isSuccess() ? PaymentStatus.PAID : PaymentStatus.FAILED);
    payment.setGatewayResponse(new HashMap<>(params)); // Lưu raw data
    paymentRepository.save(payment);

    if (result.isSuccess()) {
        bookingService.confirmPayment(payment.getBooking().getId());
        // Trigger async notification
        eventPublisher.publishEvent(new PaymentSuccessEvent(payment));
    }
}
```
### State Machine & Rules

- PENDING → PAID (Webhook success) hoặc FAILED (Timeout/Reject) hoặc CANCELLED (User hủy).
- PAID → REFUNDED (Phase 11 Cancellation).
- Tuyệt đối không cho phép PENDING → COMPLETED trực tiếp.
- Webhook PHẢI trả HTTP 200/204 ngay sau khi update DB thành công, không chờ async job.

### Error Codes
```java
PAY_001("PAYMENT_BOOKING_INVALID", "Booking không tồn tại hoặc không thể thanh toán"),
PAY_002("PAYMENT_METHOD_UNSUPPORTED", "Phương thức thanh toán không được hỗ trợ"),
PAY_003("PAYMENT_INVALID_SIGNATURE", "Chữ ký webhook không hợp lệ"),
PAY_004("PAYMENT_ALREADY_PROCESSED", "Giao dịch đã được xử lý trước đó"),
PAY_005("PAYMENT_AMOUNT_MISMATCH", "Số tiền thanh toán không khớp với giá vé"),
PAY_006("PAYMENT_GATEWAY_TIMEOUT", "Cổng thanh toán phản hồi chậm hoặc lỗi");
```
### Validation Rules
Hạng mục  | Rule  |
bookingId | Phải thuộc user đang đăng nhập, status=PENDING, chưa hết hạn reserved_until |
methodCode  | Phải tồn tại trong payment_methods & is_active=true |
amount  | Server tự lấy từ booking.final_price. FE không được truyền amount.  |
Webhook | Bắt buộc verify HMAC signature. Bỏ qua nếu signature sai. |
Idempotency | DB UNIQUE(gateway_transaction_id) + service check status != PENDING |
Timeout | |Webhook retry tối đa 3 lần từ cổng. Backend xử lý idempotent → không duplicate.  |

### Checklist Pre-PR
- Payment entity có @SQLRestriction, @Type(JsonBinaryType.class) cho gateway_response
- PaymentGatewayProvider interface + MockGatewayProvider hoạt động ổn định
- Webhook verify signature & idempotency trước khi update DB
- @Transactional bao trùm webhook handler, trả 200 OK ngay sau commit
- COD flow update booking status → CONFIRMED + invalidate seat cache
- DTOs: Request=class, Response=record, MapStruct mapping
- Không hardcode VNPAY/MoMo config trong core service (dùng application.yml + Profile)
- mvn clean compile + integration test webhook pass

### Ghi Chú Triển Khai Thực Tế
1. Mock vs Real: Hiện tại FE sẽ nhận paymentUrl dạng https://mock-pay.local/123. Click vào sẽ redirect về returnUrl với params giả lập success/failed. Khi có merchant VNPAY, chỉ cần đổi @Primary sang VnpayGatewayProvider.
2. Bảo mật Webhook: Cấu hình IP whitelist của MoMo/VNPAY trong application.yml + verify HMAC-SHA256 với secretKey.
3. Async Notification: Sau khi webhook update PAID, dùng @Async hoặc Spring Event để gửi email/SMS xác nhận vé, tránh block luồng webhook.
4. Reconciliation: Sau này cần job đối soát tự động (Phase 12 Admin) để sync payments với báo cáo cổng.
</task>
