# API Contract: Phase 7 – Booking (Đặt Vé & Giữ Ghế)

Document for FE team. All responses use `ApiResponse<T>` wrapper.

> [!IMPORTANT]
> Phase 7 includes critical anti-overbooking logic. Read the **Business Rules** section carefully before integrating.

## Base URL: `/api/v1`

---

## 🎫 Booking APIs (USER)

> Yêu cầu `Authorization: Bearer <token>`

### 1. Tạo booking mới (Giữ ghế 10 phút)
- **Endpoint**: `POST /bookings`
- **Header**: `Idempotency-Key: <UUID v4>` *(optional, nhưng khuyến khích để tránh trùng request)*
- **Auth**: USER
- **Request Body**:
```json
{
  "tripId": 1,
  "pickupOrder": 1,
  "dropoffOrder": 3,
  "passengers": [
    {
      "fullName": "Nguyễn Văn A",
      "phone": "0901234567",
      "idCard": "001234567890",
      "dateOfBirth": "1990-05-15",
      "seatRow": 1,
      "seatCol": "A"
    },
    {
      "fullName": "Trần Thị B",
      "phone": "0912345678",
      "seatRow": 1,
      "seatCol": "B"
    }
  ]
}
```
- **Validation**:
  - `pickupOrder` >= 1, `dropoffOrder` >= 2, pickup < dropoff
  - `passengers`: 1 đến 10 người, mỗi người một ghế
  - `phone`: Đúng định dạng VN (`^0[35789][0-9]{8}$`)
  - `seatRow + seatCol` → tự động map sang `seat_number` trong DB (ví dụ `1A`)
- **Success (201)**:
```json
{
  "data": {
    "bookingCode": "550e8400-e29b-41d4-a716-446655440000",
    "tripId": 1,
    "routeName": "Hồ Chí Minh – Hà Nội",
    "companyName": "Phương Trang",
    "departureDate": "2026-05-01",
    "departureTime": "22:00:00+07:00",
    "pickupOrder": 1,
    "dropoffOrder": 3,
    "totalAmount": 700000.00,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "expiredAt": "2026-04-28T23:30:00+07:00"
  }
}
```
- **Lưu ý FE**: `expiredAt` = thời điểm ghế hết hạn giữ. FE nên hiển thị countdown timer!
- **Errors**: `BOOKING_002` (ghế đã có người đặt), `BOOKING_003` (đang có người khác xử lý — thử lại sau), `BOOKING_006` (chuyến không active), `BOOKING_007` (không có giá cho đoạn đường này)

---

### 2. Lịch sử đặt vé
- **Endpoint**: `GET /bookings/my`
- **Query Params**: `page` (default 0), `size` (max 50)
- **Success (200)**: Paginated `BookingResDTO` của user đang đăng nhập

---

### 3. Chi tiết booking
- **Endpoint**: `GET /bookings/{bookingCode}`
- **Note**: `bookingCode` là UUID (ví dụ `550e8400-e29b-41d4-a716-446655440000`)
- **Success (200)**:
```json
{
  "data": {
    "booking": { ...BookingResDTO... },
    "seats": [
      {
        "seatNumber": "1A",
        "seatType": "STANDARD",
        "finalPrice": 350000.00,
        "passenger": {
          "fullName": "Nguyễn Văn A",
          "phone": "0901234567"
        }
      }
    ]
  }
}
```

---

### 4. Hủy booking
- **Endpoint**: `POST /bookings/{bookingCode}/cancel`
- **Constraints**: Chỉ hủy được khi `status = PENDING | CONFIRMED`
- **Effect**: `status → CANCELLED`, seat cache của trip được xóa ngay lập tức
- **Error**: `BOOKING_005` nếu status là EXPIRED/COMPLETED/CANCELLED

---

## 🔐 Admin APIs

### 5. Danh sách tất cả bookings
- **Endpoint**: `GET /admin/bookings`
- **Query Params**:
  - `status`: Lọc theo `PENDING | CONFIRMED | CANCELLED | EXPIRED | COMPLETED`
  - `page`, `size`
- **Success (200)**: Paginated `BookingResDTO`

---

## BookingStatus Enum

| Value | Mô tả |
|---|---|
| `PENDING` | Ghế đang bị giữ, chờ thanh toán (hết hạn sau 10 phút) |
| `CONFIRMED` | Đã thanh toán thành công |
| `CANCELLED` | User hoặc admin đã hủy |
| `EXPIRED` | Quá thời gian giữ ghế mà chưa thanh toán |
| `COMPLETED` | Chuyến đã đi xong |

## PaymentStatus Enum

| Value | Mô tả |
|---|---|
| `PENDING` | Chưa thanh toán |
| `PAID` | Đã thanh toán |
| `REFUNDED` | Đã hoàn tiền |
| `FAILED` | Thanh toán thất bại |

---

## 🏢 Vendor Booking APIs (VENDOR role)

> Vendor thấy **chỉ bookings của công ty mình** — scoped theo `company_id` qua `trip→route→company`.

### 6. Danh sách đơn đặt vé của nhà xe
- **Endpoint**: `GET /vendor/bookings`
- **Auth**: `VENDOR` role
- **Query Params**:
  - `status`: Lọc theo `PENDING | CONFIRMED | CANCELLED | EXPIRED | COMPLETED`
  - `page`, `size`
- **Success (200)**: Paginated `BookingResDTO`

### 7. Dashboard vendor (đã có booking data từ Phase 7)
- **Endpoint**: `GET /vendor/dashboard`
- **`todayRevenue`**: Tổng `final_price` bookings có `paymentStatus=PAID` ngày hôm nay
- **`pendingBookings`**: Số bookings `status=PENDING` (ghế đang bị giữ)
> ⚠️ Trước Phase 7, `todayRevenue=0` và `pendingBookings=0` là hardcoded. Từ Phase 7, là dữ liệu thực từ DB.

---

## Error Codes (Phase 7)

| Code | Mô tả |
|---|---|
| `BOOKING_001` | Mã đặt vé không tồn tại |
| `BOOKING_002` | Ghế đã được đặt hoặc không khả dụng trên đoạn đường này |
| `BOOKING_003` | Ghế đang được giữ bởi người dùng khác — thử lại sau 3 giây |
| `BOOKING_004` | Yêu cầu đã được xử lý trước đó (Idempotency hit) |
| `BOOKING_005` | Không thể hủy vé ở trạng thái này |
| `BOOKING_006` | Chuyến xe không tồn tại hoặc đã hủy |
| `BOOKING_007` | Không tìm thấy giá cho đoạn đường này |

---

## Critical Business Rules (ANTI-OVERBOOKING)

```
Flow tạo booking (5 bước):

1. IDEMPOTENCY CHECK
   - Header: Idempotency-Key: <UUID>
   - Redis key: busozy:{env}:idempotency:booking:{key}  TTL 24h
   - Nếu hit → trả kết quả cũ ngay, không cần thực hiện thêm

2. DISTRIBUTED LOCK (Redis)
   - Key: busozy:{env}:lock:trip:{tripId}  TTL 3s
   - Serialize tất cả request cho cùng một chuyến
   - Nếu không lấy được lock → lỗi BOOKING_003, FE nên retry sau 3s
   - try-finally: lock LUÔN được release, kể cả khi lỗi

3. OVERLAP CHECK (Source of Truth - Native SQL)
   - Logic: NOT (booking.dropoff_order <= pickupOrder OR booking.pickup_order >= dropoffOrder)
   - Bỏ qua bookings đã CANCELLED/EXPIRED/COMPLETED
   - Nếu có ghế bị overlap → lỗi BOOKING_002

4. PRICE CALCULATION (Server-side ONLY)
   - base_price từ route_prices theo (pickup_order, dropoff_order)
   - finalPricePerSeat = base_price × seat.price_multiplier (làm tròn HALF_UP)
   - totalAmount = tổng tất cả seat final prices
   - FE KHÔNG được tự tính hoặc gửi giá lên

5. PERSIST (@Transactional)
   - Tạo Booking (status=PENDING, reservedUntil=now+10min)
   - Tạo BookingSeat × số hành khách
   - Tạo Passenger × số hành khách
   - Commit → evict seat cache

SEAT HOLD EXPIRY:
   - @Scheduled job chạy mỗi 60s
   - Batch UPDATE PENDING → EXPIRED nơi reserved_until < NOW()
   - Invalidate seat cache của các trip bị ảnh hưởng
```

## Seat Identification (seatRow + seatCol)

```
FE gửi seatRow=1, seatCol="A" → Backend map sang seat_number="1A" trong DB
Không dùng seat_id trực tiếp từ FE để tránh tamper

Ví dụ: Bus 40 chỗ
  Row 1: 1A, 1B, 1C, 1D
  Row 2: 2A, 2B, 2C, 2D
  ...

FE nên lấy layout từ GET /trips/{id}/seats để biết seatNumber của từng ghế,
sau đó split seatNumber thành row (số đầu) và col (ký tự sau).
```
