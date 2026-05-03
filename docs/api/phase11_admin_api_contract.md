# API Contract: Phase 11 – Admin APIs & Dashboard

## Base URL: `/api/v1/admin`

Phase 11 bổ sung nhóm API cho `PLATFORM_ADMIN` để FE admin có thể:
- xem dashboard toàn hệ thống
- quản lý user/customer/vendor
- khóa hoặc mở khóa tài khoản
- xem audit trail các thao tác quản trị

## Auth Model

- Role dùng trong backend hiện tại là `PLATFORM_ADMIN`.
- Khi một user bị khóa:
  - `isBanned=true`
  - `status=BLOCKED`
  - `banReason` được lưu lại
- User bị khóa sẽ không đăng nhập mới được và refresh token cũng bị chặn.

## Endpoints

### 1. Global dashboard
- `GET /dashboard/global`
- Auth: `PLATFORM_ADMIN`

Response:
```json
{
  "data": {
    "totalBookingsToday": 32,
    "totalRevenueToday": 4850000,
    "activeTripsCount": 14,
    "pendingBookingsCount": 7,
    "topRoutes": [
      {
        "routeName": "Sai Gon - Da Lat",
        "totalBookings": 10,
        "totalRevenue": 1650000
      }
    ],
    "recentBookings": [
      {
        "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
        "tripId": 45,
        "routeName": "Sai Gon - Da Lat",
        "companyName": "Limousine A",
        "departureDate": "2026-05-02",
        "departureTime": "21:30:00+07:00",
        "pickupOrder": 1,
        "dropoffOrder": 4,
        "totalAmount": 450000,
        "status": "CONFIRMED",
        "paymentStatus": "PAID",
        "expiredAt": null
      }
    ]
  }
}
```

Notes:
- `totalRevenueToday` hiện được tính theo booking `paymentStatus=PAID` và `createdAt=today`.
- `activeTripsCount` hiện gồm các trạng thái `SCHEDULED`, `DELAYED`, `DEPARTED`.
- `topRoutes` và `recentBookings` đang trả tối đa 5 items để FE render widget/dashboard card.

### 2. Danh sách user toàn hệ thống
- `GET /users`
- Auth: `PLATFORM_ADMIN`
- Query:
  - `page`, `size`
  - `search` optional: search theo `username`, `email`, `fullName`
  - `status` optional: `ACTIVE`, `INACTIVE`, `BLOCKED`
  - `role` optional: `CUSTOMER`, `VENDOR`, `PLATFORM_ADMIN`
  - `banned` optional: `true`, `false`

Response item:
```json
{
  "id": 12,
  "username": "vendor_a",
  "email": "vendor@example.com",
  "fullName": "Vendor A",
  "phone": "0909000111",
  "role": "VENDOR",
  "userType": "VENDOR",
  "status": "BLOCKED",
  "isBanned": true,
  "banReason": "Spam booking / abuse policy",
  "companyId": 8,
  "companyName": "Limousine A",
  "createdAt": "2026-05-02T15:00:00+07:00"
}
```

### 3. Khóa hoặc mở khóa user
- `PATCH /users/{id}/ban`
- Auth: `PLATFORM_ADMIN`

Request:
```json
{
  "banned": true,
  "reason": "Spam booking / abuse policy"
}
```

Unban request:
```json
{
  "banned": false,
  "reason": null
}
```

Rules:
- Khi `banned=true`, `reason` là bắt buộc.
- Khi `banned=false`, backend sẽ clear `banReason` và set `status=ACTIVE`.
- Endpoint này tạo audit log action `BAN_USER`.
- Khi ban/unban thành công, backend sẽ xóa refresh token hiện tại của user để chặn session refresh tiếp theo.

### 4. Audit logs
- `GET /logs`
- Auth: `PLATFORM_ADMIN`
- Query:
  - `page`, `size`
  - `action` optional
  - `entityType` optional

Response item:
```json
{
  "id": 101,
  "userId": 1,
  "username": "superadmin",
  "action": "BAN_USER",
  "entityType": "User",
  "entityId": 12,
  "details": {
    "action": "BAN_USER",
    "entityType": "User",
    "entityId": 12,
    "method": "AdminUserController.updateBanStatus(..)",
    "ipAddress": "127.0.0.1",
    "userAgent": "Mozilla/5.0 ...",
    "requestPayload": {
      "banned": true,
      "reason": "Spam booking / abuse policy"
    },
    "changes": {
      "isBanned": { "old": false, "new": true },
      "banReason": { "old": null, "new": "Spam booking / abuse policy" },
      "status": { "old": "ACTIVE", "new": "BLOCKED" }
    },
    "resultType": "ApiResponse"
  },
  "ipAddress": "127.0.0.1",
  "createdAt": "2026-05-02T15:10:00+07:00"
}
```

## FE Notes

- Admin FE nên poll hoặc manual refresh dashboard thay vì assume realtime websocket.
- User list nên expose cả `status` và `isBanned`; backend hiện giữ cả hai để dễ tương thích logic auth hiện có.
- Khi admin khóa user thành công, FE nên invalidate user list và detail cache ngay.
- `BAN_USER` audit log hiện dùng chung cho cả hành động khóa và mở khóa; FE có thể phân biệt qua `details.requestPayload.banned`.
- Audit log `details` hiện có schema ổn định gồm `action`, `entityType`, `entityId`, `method`, `ipAddress`, `userAgent`, `requestPayload`, `changes`, `resultType`.

## Error Codes

| Code | Meaning |
|---|---|
| `AUTH_001` | User không tồn tại |
| `AUTH_006` | Tài khoản chưa kích hoạt hoặc đã bị khóa theo status |
| `AUTH_010` | Tài khoản đã bị admin khóa |
| `GEN_002` | Validation error, ví dụ thiếu lý do khi ban user |
