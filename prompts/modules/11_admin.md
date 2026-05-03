<task>
# Module 11 – Admin APIs & Dashboard

1. Tổng quan
Khác với Vendor (chỉ quản lý công ty của họ), Super-Admin (ROLE_ADMIN) cần nhìn thấy toàn cảnh hệ thống và có quyền can thiệp sâu (khóa user, xem log).
2. Database & Migration
Flyway Script: V13__create_activity_logs.sql
```sql
-- Bảng nhật ký hoạt động (Audit Logs)
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id), -- Null nếu là system job
    action VARCHAR(50) NOT NULL,         -- e.g., "BAN_USER", "DELETE_COMPANY", "LOGIN_FAILED"
    entity_type VARCHAR(50),             -- e.g., "User", "Company", "Route"
    entity_id BIGINT,
    details JSONB,                       -- Lưu thông tin chi tiết (IP, UserAgent, OldValue, NewValue)
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_activity_logs_created ON activity_logs(created_at DESC);
CREATE INDEX idx_activity_logs_action ON activity_logs(action);
```
Cập nhật bảng users (Nếu chưa có)
```sql
-- Thêm cột để khóa tài khoản (Banning)
ALTER TABLE users ADD COLUMN is_banned BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN ban_reason VARCHAR(255);
```
Cập nhật entity và các logic liên quan

3. Entity: ActivityLog
```java
@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    @Type(JsonBinaryType.class) private Map<String, Object> details;
    private String ipAddress;
    @CreatedDate private OffsetDateTime createdAt;
}
```
## DTOs
```
dto/response/
├── DashboardResDTO.java:
│   - totalBookingsToday: long
│   - totalRevenueToday: BigDecimal
│   - activeTripsCount: long
│   - pendingBookingsCount: long
│   - topRoutes: List<RouteStatsResDTO>
│   - recentBookings: List<BookingResDTO>
└── RouteStatsResDTO.java – routeName, totalBookings, totalRevenue
```

## API Endpoints – Dashboard
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/admin/dashboard/global` | ADMIN | Thống kê tổng toàn hệ thống (Doanh thu, số Booking, Top Company) |
| GET | `/api/v1/admin/users` | ADMIN | Danh sách tất cả User (Customer + Vendor). Có search & filter status |
| PATCH | `/api/v1/admin/users/{id}/ban` | ADMIN | Khóa/Mở khóa user (Cần lý do) |
| GET | `/api/v1/admin/logs` | ADMIN | Xem nhật ký hoạt động (Audit Trail) |


## Dashboard Query Examples
```java
// Total bookings today
@Query("SELECT COUNT(b) FROM Booking b WHERE DATE(b.createdAt) = CURRENT_DATE AND b.status != 'EXPIRED'")
long countTodayBookings();

// Revenue today (from confirmed bookings)
@Query("SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b WHERE DATE(b.paidAt) = CURRENT_DATE AND b.paymentStatus = 'PAID'")
BigDecimal sumTodayRevenue();

// Active trips
@Query("SELECT COUNT(t) FROM Trip t WHERE t.status IN ('BOARDING', 'DEPARTED', 'ON_ROUTE')")
long countActiveTrips();
```

5. Business Logic Chi Tiết
A. Global Dashboard (Thống kê hệ thống)
Vendor chỉ thấy tiền của họ, Admin cần thấy tiền của tất cả.

- Query logic:
```sql
-- Tổng doanh thu hôm nay (đã thanh toán)
SELECT SUM(final_price) FROM bookings 
WHERE payment_status = 'PAID' 
AND DATE(created_at) = CURRENT_DATE;
```
B. Audit Logs (Tự động hóa bằng AOP)
Thay vì viết code log thủ công ở mỗi Service, hãy dùng Spring AOP để tự động ghi log khi Admin thao tác.
- Code ví dụ:
```java
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final ActivityLogRepository logRepository;

    @AfterReturning(
        pointcut = "@annotation(com.tomzxy.busozy.common.annotation.Auditable)",
        returning = "result"
    )
    public void logAdminAction(JoinPoint joinPoint, Object result) {
        // Tự động lấy user hiện tại, action, entity và lưu vào DB
        // Chi tiết sẽ implement ở Phase 11
    }
}
```
- Cách dùng: Chỉ cần thêm @Auditable(action = "BAN_USER") lên hàm banUser() trong Admin Controller là xong.
C. Ban User (Khóa tài khoản)
- Khi Admin khóa user -> Set is_banned = true.
- Cơ chế chặn đăng nhập: Trong LoginService hoặc JwtFilter, kiểm tra nếu user.isBanned -> Ném exception AUTH_009 ("Tài khoản đã bị khóa").
- Vendor cũng có thể bị khóa (ngừng hoạt động trên nền tảng).

6. Checklist Pre-PR
- Migration V13 tạo bảng activity_logs.
- Thêm cột is_banned vào users.
- Implement API GET /admin/dashboard/global (Query sum không filter company).
- Implement API PATCH /admin/users/{id}/ban.
- Implement AOP @Auditable để ghi log tự động.
- mvn compile thành công.
</task>
