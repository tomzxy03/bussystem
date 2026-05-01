# API Contract: Vendor Portal

Tài liệu này mô tả các API dành cho trang quản trị vendor. Tất cả path đều có prefix `/api/v1`.

## Auth / Routing
- Vendor đăng ký qua `POST /vendor/register`
- Vendor đăng nhập qua `POST /auth/login`
- FE dùng `data.role === "VENDOR"` để route sang vendor portal
- Tất cả API dưới đây yêu cầu `Authorization: Bearer <token>` với `role = VENDOR`

## 1. Hồ sơ vendor

### GET `/vendor/me/profile`
```json
{
  "success": true,
  "data": {
    "userId": 12,
    "username": "nhaxe_abc",
    "email": "owner@abc.vn",
    "fullName": "Nguyen Van Vendor",
    "phone": "0912345678",
    "companyId": 3,
    "companyName": "Nha Xe ABC",
    "taxCode": "0123456789",
    "companyPhone": "0988888888",
    "companyAddress": "123 Ben Xe Mien Dong",
    "createdAt": "2026-04-27T10:00:00+07:00"
  }
}
```

### PUT `/vendor/me/profile`
```json
{
  "fullName": "Nguyen Van Vendor",
  "email": "owner@abc.vn",
  "phone": "0912345678",
  "companyName": "Nha Xe ABC",
  "taxCode": "0123456789",
  "companyPhone": "0988888888",
  "address": "123 Ben Xe Mien Dong"
}
```

### GET `/vendor/dashboard`
```json
{
  "success": true,
  "data": {
    "activeTrips": 8,
    "activeBuses": 5,
    "totalDrivers": 9,
    "todayRevenue": 0,
    "pendingBookings": 0
  }
}
```

`todayRevenue` và `pendingBookings` hiện đang để `0` vì module booking/payment chưa có.

## 2. Nhà xe

### GET `/vendor/company`
Trả về `CompanyResDTO` của công ty vendor đang quản lý.

### PUT `/vendor/company`
```json
{
  "name": "Nha Xe ABC",
  "taxCode": "0123456789",
  "phone": "0988888888",
  "address": "123 Ben Xe Mien Dong"
}
```

## 3. Tài xế

### GET `/vendor/drivers`

### POST `/vendor/drivers`
### PUT `/vendor/drivers/{id}`
```json
{
  "companyId": 999,
  "fullName": "Tai Xe A",
  "phone": "0911111111",
  "licenseNumber": "B2ABC123",
  "avatarUrl": null,
  "dateOfBirth": "1990-01-01",
  "address": "Thu Duc"
}
```

### PATCH `/vendor/drivers/{id}/status?status=ACTIVE`

Ghi chú:
- FE có thể gửi `companyId`, nhưng backend sẽ luôn ghi đè bằng công ty của vendor hiện tại.

## 4. Xe

### GET `/vendor/buses`
Query params:
- `status`
- `keyword`
- `page`
- `size`
- `sort`

### POST `/vendor/buses`
### PUT `/vendor/buses/{id}`
```json
{
  "companyId": 999,
  "busTypeId": 1,
  "seatLayoutId": 2,
  "licensePlate": "51B-12345",
  "busNumber": "XE-01",
  "name": "Limousine 34 phòng",
  "status": "ACTIVE"
}
```

### PATCH `/vendor/buses/{id}/status?status=MAINTENANCE`
### POST `/vendor/buses/{id}/seats`

## 5. Tuyến

### GET `/vendor/routes`
Query params:
- `keyword`
- `page`
- `size`
- `sort`

### POST `/vendor/routes`
### PUT `/vendor/routes/{id}`
```json
{
  "code": "SG_DALAT_01",
  "name": "Sai Gon - Da Lat",
  "distanceKm": 305.5,
  "durationMinutes": 420,
  "companyId": 999
}
```

### PUT `/vendor/routes/{id}/stops`
### PUT `/vendor/routes/{id}/prices`
### DELETE `/vendor/routes/{id}`

## 6. Chuyến

### GET `/vendor/trips`
Query params:
- `status`
- `departureDate`
- `page`
- `size`
- `sort`

### POST `/vendor/trips`
### PUT `/vendor/trips/{id}`
```json
{
  "routeId": 10,
  "busId": 22,
  "driverId": 35,
  "departureDate": "2026-05-01",
  "departureTime": "22:00:00+07:00"
}
```

### PATCH `/vendor/trips/{id}/status?status=SCHEDULED`
### DELETE `/vendor/trips/{id}`

## Scope Rules Quan Trọng
- Vendor chỉ đọc/ghi được dữ liệu thuộc `companyId` của chính họ.
- Nếu truy cập `driver/bus/route/trip` của company khác, backend trả `403`.
- Các API vận hành cũ dưới `/api/v1/admin/buses`, `/api/v1/admin/routes`, `/api/v1/admin/trips`, `/api/v1/admin/drivers` không còn dùng cho FE.
- `PLATFORM_ADMIN` hiện chỉ còn quản lý `company`; `stop` vẫn là shared/global resource do platform quản lý.
