# API Contract: Phase 12 – Cancellation & Refund

## Base URLs

- User APIs: `/api/v1/bookings`
- Admin APIs: `/api/v1/admin`

Phase 12 bổ sung luồng hủy vé có policy hoàn tiền, lưu lịch sử cancellation, và endpoint admin để theo dõi/xử lý refund retry.

## Auth Model

- User APIs yêu cầu user sở hữu booking.
- Admin APIs yêu cầu role `PLATFORM_ADMIN`.

## Core Behavior

- Chỉ cho phép hủy booking ở trạng thái `PENDING` hoặc `CONFIRMED`.
- Nếu booking chưa thanh toán (`paymentStatus != PAID`), backend vẫn cho hủy nhưng `refundAmount = 0`.
- Nếu booking đã thanh toán và đủ điều kiện hoàn tiền, cancellation được tạo với `refundStatus = PENDING`, sau đó backend xử lý refund async.
- `refundAmount` luôn bị cap bởi `payment.amount` thực tế đã lưu, không hoàn vượt số tiền đã thu.
- Nếu booking đã confirm với promotion trước đó, khi cancel backend sẽ rollback lại `promotions.used_count` và `user_promotion_usage.used_count`.
- Khi refund thành công:
  - `cancellation.refundStatus = COMPLETED`
  - `booking.paymentStatus = REFUNDED`
- Nếu refund fail:
  - `cancellation.refundStatus = FAILED`
  - admin có thể gọi retry qua endpoint `process-refund`

## User Endpoints

### 1. Cancellation preview
- `GET /bookings/{code}/cancellation-preview`
- Auth: user sở hữu booking

Response:
```json
{
  "data": {
    "bookingId": 45,
    "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
    "originalAmount": 450000,
    "refundAmount": 225000,
    "refundPercentage": 50.0,
    "policyDescription": "Hoàn 50% nếu hủy trước ít nhất 24 giờ. Scope áp dụng: company+route. Còn 31 giờ trước giờ khởi hành.",
    "departureTime": "2026-05-05T21:30:00+07:00"
  }
}
```

Notes:
- Nếu booking chưa thanh toán, `refundAmount = 0` và `refundPercentage = 0`.
- `policyDescription` là text để FE render trực tiếp cho confirmation modal/card.
- Nếu refund policy tính ra lớn hơn số tiền đã thu thực tế, response preview sẽ trả số tiền đã được cap.

### 2. Cancel booking
- `POST /bookings/{code}/cancel`
- Auth: user sở hữu booking

Request:
```json
{
  "reason": "Đổi lịch cá nhân",
  "confirmRefundPolicy": true
}
```

Response:
```json
{
  "data": {
    "cancellationId": 12,
    "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
    "refundAmount": 225000,
    "refundStatus": "PENDING",
    "cancelReason": "Đổi lịch cá nhân",
    "cancelTime": "2026-05-03T14:10:00+07:00",
    "cancelledByUsername": "customer_a"
  }
}
```

Notes:
- `confirmRefundPolicy` phải là `true`.
- Nếu booking chưa thanh toán hoặc không đủ điều kiện hoàn tiền, `refundStatus` sẽ là `COMPLETED` ngay và `refundAmount = 0`.

### 3. Cancellation detail
- `GET /bookings/{code}/cancellation`
- Auth: user sở hữu booking

Response:
```json
{
  "data": {
    "cancellationId": 12,
    "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
    "refundAmount": 225000,
    "refundStatus": "COMPLETED",
    "cancelReason": "Đổi lịch cá nhân",
    "cancelTime": "2026-05-03T14:10:00+07:00",
    "cancelledByUsername": "customer_a"
  }
}
```

## Admin Endpoints

### 4. Cancellation list
- `GET /admin/cancellations`
- Auth: `PLATFORM_ADMIN`
- Query:
  - `page`, `size`
  - `bookingCode` optional
  - `refundStatus` optional: `PENDING`, `PROCESSED`, `FAILED`, `COMPLETED`

Response item:
```json
{
  "cancellationId": 12,
  "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
  "refundAmount": 225000,
  "refundStatus": "FAILED",
  "cancelReason": "Đổi lịch cá nhân",
  "cancelTime": "2026-05-03T14:10:00+07:00",
  "cancelledByUsername": "customer_a"
}
```

### 5. Retry / process refund
- `POST /admin/cancellations/{id}/process-refund`
- Auth: `PLATFORM_ADMIN`

Response:
```json
{
  "data": {
    "cancellationId": 12,
    "bookingCode": "30e8c5f5-44d0-4a74-b86f-2d4c3630b118",
    "refundAmount": 225000,
    "refundStatus": "COMPLETED",
    "cancelReason": "Đổi lịch cá nhân",
    "cancelTime": "2026-05-03T14:10:00+07:00",
    "cancelledByUsername": "customer_a"
  }
}
```

Notes:
- FE nên chỉ show nút retry khi `refundStatus = FAILED` hoặc `PENDING`.

### 6. Cancellation policy list
- `GET /admin/cancellation-policies`
- Auth: `PLATFORM_ADMIN`
- Query:
  - `page`, `size`
  - `companyId` optional
  - `routeId` optional
  - `active` optional

Response item:
```json
{
  "id": 3,
  "companyId": 8,
  "companyName": "Limousine A",
  "routeId": 21,
  "routeName": "Sai Gon - Da Lat",
  "hoursBeforeDeparture": 24,
  "refundPercentage": 50.00,
  "isActive": true,
  "createdAt": "2026-05-03T13:00:00+07:00",
  "updatedAt": "2026-05-03T13:00:00+07:00"
}
```

### 7. Create cancellation policy
- `POST /admin/cancellation-policies`
- Auth: `PLATFORM_ADMIN`

Request:
```json
{
  "companyId": 8,
  "routeId": 21,
  "hoursBeforeDeparture": 24,
  "refundPercentage": 50.00,
  "isActive": true
}
```

### 8. Update cancellation policy
- `PUT /admin/cancellation-policies/{id}`
- Auth: `PLATFORM_ADMIN`

Request body giống `POST`.

## Policy Resolution Rules

Backend resolve policy theo thứ tự:
1. `company + route`
2. `company only`
3. `route only`
4. `global`

Trong mỗi scope, backend ưu tiên policy có `hoursBeforeDeparture` cao nhất nhưng vẫn thỏa điều kiện thời gian hiện tại.

## Error Codes

| Code | Meaning |
|---|---|
| `BOOKING_001` | Booking không tồn tại |
| `CANCEL_001` | Lịch sử hủy vé không tồn tại |
| `CANCEL_002` | Vé đã bị hủy trước đó |
| `CANCEL_003` | Không thể hủy ở trạng thái hiện tại hoặc đã qua giờ khởi hành |
| `CANCEL_004` | Không tìm thấy policy hoàn tiền |
| `CANCEL_005` | Xử lý hoàn tiền thất bại |
| `CANCEL_006` | User không có quyền trên booking này |
| `GEN_002` | Validation error, ví dụ `confirmRefundPolicy=false` hoặc `reason` chứa HTML |

## FE Notes

- Flow đề xuất:
  1. gọi `cancellation-preview`
  2. hiển thị policy + refund amount
  3. user tick confirm checkbox
  4. submit `cancel`
- Sau khi cancel thành công, FE nên invalidate:
  - booking detail
  - booking history
  - payment status nếu đang hiển thị
- Với refund async:
  - FE nên poll lại `GET /bookings/{code}/cancellation` nếu response ban đầu là `PENDING`
  - trạng thái terminal hiện tại là `COMPLETED` hoặc `FAILED`
