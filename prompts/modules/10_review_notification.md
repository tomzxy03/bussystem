<task>
# Module 10 – Review & Notification

## Prerequisite
- Module `07_booking` đã hoàn thành.
- Đã áp dụng @EnableAsync và Spring Events (@TransactionalEventListener) từ Phase 8.
- Tuân thủ global_standards.md (Spring Boot 3.3.5, @SQLRestriction, ApiResponse, Redis busozy:${app.env}:..., MapStruct, Constructor Injection).
- Hiểu rõ luồng: Notification được trigger bởi System Events (Booking, Payment, Trip status), xử lý async, có retry mechanism. Review chỉ mở khi trip hoàn thành.

## Database Tables
```sql
-- 1. Reviews (Soft delete, unique per booking)
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE RESTRICT,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    is_verified BOOLEAN DEFAULT FALSE,          -- Auto-verify sau 24h hoặc Admin duyệt
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_reviews_trip_created ON reviews(trip_id, created_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_user ON reviews(user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_reviews_booking_unique ON reviews(booking_id) WHERE deleted_at IS NULL;

-- 2. Notifications (Async queue, no soft-delete for MVP)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,               -- SYSTEM, EMAIL, SMS
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,                             -- Deep-link payload: {"bookingId": 123, "tripId": 45}
    status VARCHAR(20) DEFAULT 'PENDING',       -- PENDING, SENT, FAILED, READ
    retry_count INT DEFAULT 0,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status) WHERE status != 'READ';
CREATE INDEX idx_notifications_pending_retry ON notifications(status, retry_count) WHERE status IN ('PENDING', 'FAILED') AND retry_count < 3;
```

## Entities

**`Review`** 
```java
@Entity @Table(name = "reviews") @SQLRestriction("deleted_at IS NULL")
public class Review extends BaseEntity {
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "trip_id", nullable = false) private Trip trip;
    @OneToOne(fetch = LAZY) @JoinColumn(name = "booking_id", nullable = false, unique = true) private Booking booking;
    
    @Column(nullable = false) @Min(1) @Max(5) private Integer rating;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "is_verified") private Boolean isVerified = false;
}
```

**`Notification`** 
```java
@Entity @Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class) // Chỉ track createdAt
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Type(JsonBinaryType.class) private Map<String, Object> metadata; // Deep-link data
    
    @Enumerated(EnumType.STRING) @Column(nullable = false) 
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(name = "retry_count") private Integer retryCount = 0;
    private OffsetDateTime sentAt, readAt;
    @CreatedDate private OffsetDateTime createdAt;
}

public enum NotificationChannel { SYSTEM, EMAIL, SMS }
public enum NotificationStatus { PENDING, SENT, FAILED, READ }
```
## DTOs
```
dto/request/
└── ReviewReqDTO.java 
```
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewReqDTO {
    @NotNull private Long bookingId;
    @NotNull @Min(1) @Max(5) private Integer rating;
    @Size(max = 1000) private String comment;
}
```
```
dto/response/
├── ReviewResDTO.java
└── NotificationResDTO.java 
```
```java
public record ReviewResDTO(
    Long id, Long userId, String username, Long tripId, String routeName,
    Integer rating, String comment, Boolean isVerified, OffsetDateTime createdAt
) {}

public record NotificationResDTO(
    Long id, NotificationChannel channel, String title, String content,
    Map<String, Object> metadata, NotificationStatus status, 
    OffsetDateTime sentAt, OffsetDateTime readAt, OffsetDateTime createdAt
) {}

public record UnreadCountResDTO(long count) {}
```
```
mapper/
├── ReviewNotificationMapper.java
```
```java
@Mapper(config = MapperConfig.class)
public interface ReviewNotificationMapper {
    ReviewResDTO toReviewRes(Review r);
    NotificationResDTO toNotificationRes(Notification n);
    default String mapUsername(User u) { return u != null ? u.getUsername() : null; }
    default String mapRouteName(Trip t) { return t != null && t.getRoute() != null ? t.getRoute().getName() : null; }
}
```

## API Endpoints
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/reviews` | USER | Đánh giá chuyến xe |
| GET | `/api/v1/trips/{id}/reviews` | Public | Reviews của chuyến (paginated, sort by createdAt,desc) |
| GET | `/api/v1/users/me/reviews` | USER | Reviews của user hiện tại |
| DELETE | `/api/v1/reviews/{id}` | USER | Xóa review của mình |
| GET | `/api/v1/notifications/my` | USER | Danh sách thông báo (filter status=PENDING,SENT) |
| PATCH | `/api/v1/notifications/{id}/read` | USER | Đánh dấu đã đọc |
| PATCH | `/api/v1/notifications/read-all` | USER | Đánh dấu tất cả đã đọc |
| GET | `/api/v1/admin/reviews` | ADMIN | Quản lý reviews |
| DELETE | `/api/v1/admin/reviews/{id}` | ADMIN | Xóa review (spam) |

## Business Logic & Architecture

### Event-Driven Notification Flow
- System không gọi trực tiếp NotificationService trong transaction chính. Dùng Spring Events để đảm bảo ACID & Async:
```java
// Listener ví dụ cho Booking Confirmed
@Component @RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        Map<String, Object> meta = Map.of("bookingId", event.bookingId(), "type", "BOOKING");
        notificationService.sendAsync(
            event.userId(), 
            NotificationChannel.SYSTEM, 
            "Đặt vé thành công!", 
            "Chuyến xe của bạn đã được xác nhận. Mã: " + event.bookingCode(),
            meta
        );
    }
}
```

### Async Processing & Retry Strategy
- NotificationService xử lý bất đồng bộ, lưu PENDING → gọi provider (Email/SMS/System) → update SENT hoặc FAILED.
   - Retry: @Scheduled(fixedDelay = 30_000) quét status=FAILED và retry_count < 3 → retry → tăng retry_count.
   - Dead Letter: Sau 3 lần fail → giữ nguyên FAILED để admin kiểm tra log.
   - Template Strategy: Chuẩn bị interface NotificationTemplateProvider để render nội dung động theo sự kiện.

### Review Eligibility & Moderation
- Điều kiện review: booking.status == COMPLETED && booking.user_id == currentUserId && chưa tồn tại review cho booking_id.
- Auto-verify: @Scheduled mỗi 24h set is_verified = true cho review mới (hoặc admin duyệt thủ công nếu cần kiểm duyệt nội dung).
- Tính điểm trung bình tuyến: Cache avg_rating và review_count ở Trip hoặc Route để tránh AVG() query nặng. Invalidate khi có review mới.

## Error Codes
```java
REV_001("REVIEW_NOT_FOUND", "Đánh giá không tồn tại"),
REV_002("BOOKING_NOT_COMPLETED", "Chỉ có thể đánh giá chuyến đã hoàn thành"),
REV_003("REVIEW_ALREADY_EXISTS", "Bạn đã đánh giá chuyến này rồi"),
REV_004("REVIEW_ACCESS_DENIED", "Bạn không có quyền xóa/sửa đánh giá này"),

NOTIF_001("NOTIFICATION_NOT_FOUND", "Thông báo không tồn tại"),
NOTIF_002("CHANNEL_UNSUPPORTED", "Kênh thông báo chưa được hỗ trợ");
```

## Validation Rules
Field |  Rule  |  Ghi chú  |
rating   |  1 <= rating <= 5  |  DTO @Min/@Max + DB CHECK constraint |
comment  |  Max 1000 chars, strip HTML tags  |  Chống XSS   |
bookingId   |  Phải thuộc user, status=COMPLETED, chưa có review  |  Service validation   |
metadata |  JSONB, tối đa 2KB |  Dùng cho deep-link FE   |
unread count   |  Cache Redis 5m (busozy:${env}:notif:unread:{userId})  |  Invalidate khi mark-read/send new   |

## Checklist Pre-PR
- Review có @SQLRestriction, unique booking_id, rating range constraint
- Notification dùng @Type(JsonBinaryType.class) cho metadata, không soft-delete
- Notification trigger qua @TransactionalEventListener(phase = AFTER_COMMIT) + @Async
- Retry mechanism cho FAILED notifications (max 3 lần)
- Cache avg_rating cho Route/Trip, invalidate khi review mới
- DTOs: Request=class + validation, Response=record
- Endpoint GET /notifications/unread/count hoạt động chính xác
- Admin có thể verify/flag review spam
- mvn clean compile + integration test event flow pass
</task>
