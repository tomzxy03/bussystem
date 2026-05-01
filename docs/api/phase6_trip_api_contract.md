# API Contract: Phase 6 – Trip & Availability

Document for FE team. All responses use `ApiResponse<T>` wrapper.

## Base URL: `/api/v1`

---

## 🚌 Trip APIs (Public)

### 1. Tìm kiếm chuyến xe
- **Endpoint**: `GET /trips/search`
- **Auth**: Public
- **Query Params**:

| Param | Type | Required | Description |
|---|---|---|---|
| `originStopId` | Long | ✅ | ID điểm đón |
| `destStopId` | Long | ✅ | ID điểm trả |
| `date` | Date (`YYYY-MM-DD`) | ✅ | Ngày khởi hành |
| `passengers` | Int | default 1 | Số hành khách |
| `page` | Int | default 0 | Trang |
| `size` | Int | default 20 | Kích thước |
| `sort` | String | default `departureTime,asc` | Sắp xếp |

- **Logic**: Tìm tuyến có điểm đón (isPickup=true) với `stop_order` < điểm trả (isDropoff=true); chỉ trả về chuyến **chưa hủy / chưa hoàn thành**.
- **Success Response (200)**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "routeCode": "SG_HN_01",
        "routeName": "Hồ Chí Minh – Hà Nội",
        "companyName": "Phương Trang",
        "busLicensePlate": "51B-12345",
        "busTypeName": "Giường nằm 40 chỗ",
        "driverName": "Nguyễn Văn A",
        "departureDate": "2026-05-01",
        "departureTime": "22:00:00+07:00",
        "status": "SCHEDULED"
      }
    ],
    "totalElements": 5
  }
}
```

---

### 2. Chi tiết chuyến xe
- **Endpoint**: `GET /trips/{id}`
- **Auth**: Public
- **Success Response (200)**:
```json
{
  "data": {
    "trip": { ...TripResDTO... },
    "segments": [
      { "pickupOrder": 1, "dropoffOrder": 2, "totalSeats": 40, "availableSeats": 38 }
    ],
    "minPrice": 200000,
    "maxPrice": 350000
  }
}
```

---

### 3. Sơ đồ ghế + trạng thái (Seat Picker UI)
- **Endpoint**: `GET /trips/{id}/seats`
- **Auth**: Public
- **Caching**: Redis TTL **15 phút** (`busozy:{env}:trip:seats:{tripId}`)
- **Cache được xóa**: Khi booking thành công hoặc hủy booking (Phase 7).
- **Query Params** *(optional)*:
  - `pickupOrder`: INT - Điểm đón (thứ tự stop)
  - `dropoffOrder`: INT - Điểm trả (thứ tự stop)
- **Success Response (200)**:
```json
{
  "data": [
    {
      "seatId": 100,
      "seatNumber": "1A",
      "seatType": "STANDARD",
      "rowNum": 1,
      "colNum": 1,
      "priceMultiplier": 1.00,
      "status": "AVAILABLE"
    },
    {
      "seatId": 101,
      "seatNumber": "1B",
      "seatType": "VIP",
      "rowNum": 1,
      "colNum": 2,
      "priceMultiplier": 1.20,
      "status": "BOOKED"
    }
  ]
}
```
> **⚠️ Note**: `status` = `AVAILABLE` | `BOOKED`. Computed dynamically tại DB — không lấy từ bảng seats. Phase 7 sẽ bổ sung LEFT JOIN vào `booking_seats` để tính chính xác theo segment (ghế BOOKED nếu booking trùng segment).

---

## Trip Ownership

- `trip` là dữ liệu thuộc `company`.
- FE không dùng `/api/v1/admin/trips` nữa.
- Vendor quản lý chuyến qua `/api/v1/vendor/trips`.
- Khi tạo/cập nhật trip, `routeId`, `busId`, `driverId` đều phải thuộc cùng company của vendor; nếu không backend trả `403`.

---

## Error Codes (Phase 6)

| Code | Mô tả |
|---|---|
| `TRIP_001` | Chuyến xe không tồn tại |
| `TRIP_002` | Xe đã được gán cho chuyến khác trong cùng ngày |
| `TRIP_003` | Tài xế không thuộc công ty quản lý chuyến |
| `TRIP_004` | Điểm đi/đến không thuộc tuyến đường |
| `TRIP_005` | Chỉ được xóa chuyến ở trạng thái DRAFT |
| `TRIP_006` | Phân đoạn ghế đã hết chỗ |

---

## Seat Availability Logic (Quan trọng cho FE)

```
Khi hiển thị seat picker cho một chuyến:
1. FE gọi GET /trips/{id}/seats?pickupOrder=1&dropoffOrder=3
2. Backend kiểm tra ghế nào đã được đặt trong segment (1→3)
   - Ghế BOOKED nếu có booking với overlap: NOT (b.dropoff_order <= 1 OR b.pickup_order >= 3)
   - Ghế AVAILABLE nếu không tìm thấy booking overlap
3. Cache 15 phút, invalidate ngay khi booking/cancel
4. Trước khi INSERT booking_seats: luôn re-check DB (không dựa vào cache)
```
