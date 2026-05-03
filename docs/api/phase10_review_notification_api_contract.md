# API Contract: Phase 10 – Review & Notification

## Base URL: `/api/v1`

Module này bổ sung hai phần cho FE:
- Review chỉ mở khi booking đã ở trạng thái `COMPLETED`.
- Notification được tạo bất đồng bộ từ các system events: booking confirmed, payment success, trip completed.

## User Endpoints

### 1. Tạo review
- `POST /reviews`
- Auth: USER
- Điều kiện:
  - `bookingId` phải thuộc user hiện tại
  - `booking.status` phải là `COMPLETED`
  - mỗi booking chỉ được review một lần

Request:
```json
{
  "bookingId": 123,
  "rating": 5,
  "comment": "Xe chạy đúng giờ, tài xế hỗ trợ tốt."
}
```

Response `201`:
```json
{
  "data": {
    "id": 10,
    "userId": 5,
    "username": "tomzxy",
    "tripId": 45,
    "routeName": "Sai Gon - Da Lat",
    "rating": 5,
    "comment": "Xe chạy đúng giờ, tài xế hỗ trợ tốt.",
    "isVerified": false,
    "createdAt": "2026-05-02T14:30:00+07:00"
  }
}
```

Note:
- Public endpoints không trả reviewer identity thật. `username` sẽ được ẩn danh hóa và `userId` không được expose.
- `rating` là số nguyên từ `1` đến `5`.

### 2. Reviews của một trip
- `GET /trips/{id}/reviews`
- Auth: Public
- Query: `page`, `size`
- Sort mặc định: `createdAt,desc`
- `username` là alias ẩn danh dạng `user_{id}`.
- `userId` sẽ là `null` ở public response.

### 3. Thống kê rating của một trip
- `GET /trips/{id}/rating-stats`
- Auth: Public

Response:
```json
{
  "data": {
    "averageRating": 4.8,
    "totalReviews": 15,
    "ratingDistribution": {
      "5": 10,
      "4": 3,
      "3": 1,
      "2": 1,
      "1": 0
    }
  }
}
```

### 4. Reviews của user hiện tại
- `GET /users/me/reviews`
- Auth: USER

### 5. Xóa review của mình
- `DELETE /reviews/{id}`
- Auth: USER
- Soft delete.

## Notification Endpoints

### 6. Danh sách thông báo của tôi
- `GET /notifications/my`
- Auth: USER
- Query:
  - `page`, `size`
  - `status` optional, csv: `PENDING,SENT,FAILED,READ`
- Nếu FE không truyền `status`, backend trả các trạng thái `PENDING`, `SENT`, `FAILED`.

Sample response:
```json
{
  "data": {
    "content": [
      {
        "id": 88,
        "channel": "SYSTEM",
        "title": "Thanh toán thành công",
        "content": "Thanh toán cho mã đặt vé 30e8c5f5-44d0-4a74-b86f-2d4c3630b118 đã hoàn tất.",
        "metadata": {
          "paymentId": 9,
          "bookingId": 123,
          "type": "PAYMENT_SUCCESS"
        },
        "status": "SENT",
        "sentAt": "2026-05-02T14:01:00+07:00",
        "readAt": null,
        "createdAt": "2026-05-02T14:01:00+07:00"
      }
    ]
  }
}
```

### 7. Unread count
- `GET /notifications/unread/count`
- Auth: USER
- FE dùng endpoint này để render badge.
- Backend cache Redis 5 phút và tự invalidate khi có notification mới hoặc mark read.

Response:
```json
{
  "data": {
    "count": 3
  }
}
```

### 8. Mark một notification là đã đọc
- `PATCH /notifications/{id}/read`
- Auth: USER

### 9. Mark toàn bộ là đã đọc
- `PATCH /notifications/read-all`
- Auth: USER

## Admin Endpoints

### 10. Danh sách review moderation
- `GET /admin/reviews`
- Auth: PLATFORM_ADMIN

### 11. Verify review
- `PATCH /admin/reviews/{id}/verify`
- Auth: PLATFORM_ADMIN
- Dùng khi admin muốn duyệt sớm thay vì chờ job auto-verify sau 24h.

### 12. Ẩn review spam
- `DELETE /admin/reviews/{id}`
- Auth: PLATFORM_ADMIN
- Soft delete: set `deleted_at`, đồng thời reset `is_verified=false` để review không còn hiển thị public nhưng vẫn giữ audit trail.

## FE Processing Notes

### Review flow
1. FE chỉ show CTA review khi booking detail trả trạng thái `COMPLETED`.
2. Submit review xong, disable CTA cho booking đó hoặc refresh booking/review list.
3. `isVerified=false` không phải lỗi. FE có thể hiển thị nhãn `Đang kiểm duyệt`.

### Notification flow
1. Sau login, FE nên gọi `GET /notifications/unread/count`.
2. Khi mở drawer/list thông báo, gọi `GET /notifications/my?page=0&size=20`.
3. Khi user click một item, gọi `PATCH /notifications/{id}/read` rồi điều hướng theo `metadata`.
4. `metadata.type` hiện có:
   - `BOOKING_CONFIRMED`
   - `PAYMENT_SUCCESS`
   - `REVIEW_REQUEST`
5. Deep-link payload hiện chứa `bookingId`, `paymentId`, `tripId` tùy loại thông báo.
6. FE có thể assume metadata shape:

```ts
interface NotificationMetadata {
  bookingId?: number;
  paymentId?: number;
  tripId?: number;
  type: 'BOOKING_CONFIRMED' | 'PAYMENT_SUCCESS' | 'REVIEW_REQUEST';
}
```

## Error Codes

| Code | Meaning |
|---|---|
| `REV_001` | Review không tồn tại |
| `REV_002` | Booking chưa hoàn thành, chưa được review |
| `REV_003` | Booking đã có review trước đó |
| `REV_004` | User không có quyền thao tác review này |
| `NOTIF_001` | Notification không tồn tại hoặc không thuộc user |
| `NOTIF_002` | Kênh gửi chưa được hỗ trợ |
