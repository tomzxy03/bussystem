# API Contract: Phase 4 – Route & Pricing (Tuyến đường & Bảng giá)

Document for FE. All responses use standard `ApiResponse<T>` wrapper.

## Base URL: `/api/v1`

---

## 📍 Route APIs (Public)

### 1. Tìm kiếm tuyến đường
- **Endpoint**: `GET /routes`
- **Auth**: Public
- **Query Params**:

| Param | Type | Description |
|---|---|---|
| `keyword` | String | Tìm theo tên hoặc mã tuyến |
| `companyId` | Long | Lọc theo công ty |
| `originStopId` | Long | ID điểm đón (dùng cùng destStopId) |
| `destStopId` | Long | ID điểm trả (dùng cùng originStopId) |
| `page` | Int | Default: 0 |
| `size` | Int | Default: 20, max: 100 |
| `sort` | String | Default: `distanceKm,asc` |

> **Lưu ý**: Khi dùng `originStopId` + `destStopId`, hệ thống tìm các tuyến mà điểm đón xuất hiện **trước** điểm trả (theo `stop_order`). Bắt buộc dùng 2 params cùng nhau.

- **Success Response (200)**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "code": "SG_HN_01",
        "name": "Hồ Chí Minh – Hà Nội",
        "companyName": "Phương Trang",
        "distanceKm": 1726.50,
        "durationMinutes": 1980,
        "isActive": true,
        "createdAt": "2026-04-25T..."
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "page": 0,
    "size": 20
  }
}
```

---

### 2. Chi tiết tuyến + điểm dừng + giá
- **Endpoint**: `GET /routes/{id}`
- **Auth**: Public
- **Caching**: Redis TTL **2h** (`busozy:{env}:route:{id}`)
- **Success Response (200)**:
```json
{
  "data": {
    "route": {
      "id": 1,
      "code": "SG_HN_01",
      "name": "Hồ Chí Minh – Hà Nội",
      "companyName": "Phương Trang",
      "distanceKm": 1726.50,
      "durationMinutes": 1980,
      "isActive": true,
      "createdAt": "..."
    },
    "stops": [
      {
        "stopOrder": 1,
        "stopName": "Bến xe Miền Đông",
        "provinceName": "Hồ Chí Minh",
        "minutesFromOrigin": 0,
        "distanceFromOrigin": 0.00,
        "isPickup": true,
        "isDropoff": false
      },
      {
        "stopOrder": 2,
        "stopName": "Bến xe Giáp Bát",
        "provinceName": "Hà Nội",
        "minutesFromOrigin": 1980,
        "distanceFromOrigin": 1726.50,
        "isPickup": false,
        "isDropoff": true
      }
    ],
    "prices": [
      {
        "pickupOrder": 1,
        "dropoffOrder": 2,
        "price": 350000.00,
        "currency": "VND"
      }
    ]
  }
}
```

---

### 3. Bảng giá theo tuyến
- **Endpoint**: `GET /routes/{id}/prices`
- **Auth**: Public
- **Success Response (200)**: Array of `RoutePriceResDTO`
  - `pickupOrder`: Thứ tự điểm đón
  - `dropoffOrder`: Thứ tự điểm trả
  - `price`: Giá vé
  - `currency`: Đơn vị tiền (mặc định `VND`)

> **⚠️ FE Note**: Giá vé được xác định bằng `pickup_order` và `dropoff_order` — **KHÔNG** bằng `stop_id`. Khi lấy giá, cần map stop → order trước.

---

## Route Ownership

- Route là dữ liệu thuộc `company`.
- FE không dùng `/api/v1/admin/routes` nữa.
- Vendor quản lý tuyến qua `/api/v1/vendor/routes`.
- Contract route write APIs hiện được chuyển sang [vendor_api_contract.md](/home/tomzxy/projects/bussystem/docs/api/vendor_api_contract.md:1).
- **Success (200)**: Full `RouteDetailResDTO`
- **Errors**: `ROUTE_003` (thứ tự không hợp lệ), `LOC_006` (stop không tồn tại)

---

### 7. Replace bảng giá ⚠️ Replace-All
- **Endpoint**: `PUT /admin/routes/{id}/prices`
- **Important**: **TOÀN BỘ** bảng giá cũ bị xóa, thay bằng mới.
- **Request Body**: Array of RoutePriceReqDTO
```json
[
  {
    "pickupOrder": 1,
    "dropoffOrder": 2,
    "price": 350000,
    "currency": "VND"
  }
]
```
- **Validation**:
  - `pickupOrder` < `dropoffOrder` (bắt buộc)
  - `pickupOrder` và `dropoffOrder` phải là `stop_order` hợp lệ trong route
  - `price` >= 1000 VND
- **Success (200)**: Full `RouteDetailResDTO`
- **Errors**: `ROUTE_005` (pickup >= dropoff), `ROUTE_004` (order không tồn tại trong route)

---

### 8. Xóa mềm tuyến
- **Endpoint**: `DELETE /admin/routes/{id}`
- **Effect**: Set `deleted_at`, `is_active = false`. Cache bị xóa.
- **Success (200)**: `{ "success": true, "message": "Xóa thành công" }`

---

## Error Codes (Phase 4)

| Code | Mô tả |
|---|---|
| `ROUTE_001` | Tuyến đường không tồn tại |
| `ROUTE_002` | Mã tuyến đã được sử dụng |
| `ROUTE_003` | Thứ tự điểm dừng không hợp lệ hoặc trùng lặp |
| `ROUTE_004` | Điểm đi/đến không thuộc danh sách dừng của tuyến |
| `ROUTE_005` | pickup_order phải nhỏ hơn dropoff_order |
| `ROUTE_006` | Công ty chủ quản chưa được kích hoạt |

---

## Pricing Logic (QUAN TRỌNG CHO FE)

```
stop_order=1 (Bến xe Miền Đông - HCM)
stop_order=2 (Đà Nẵng)
stop_order=3 (Bến xe Giáp Bát - HN)

Giá hợp lệ:
- (pickup=1, dropoff=2): HCM → Đà Nẵng = 200.000 VND
- (pickup=1, dropoff=3): HCM → HN = 350.000 VND
- (pickup=2, dropoff=3): Đà Nẵng → HN = 180.000 VND

Khi hiển thị giá vé cho hành khách:
1. FE nhận stops list (có stop_order)
2. Hành khách chọn điểm đón/trả
3. FE map chọn lựa → pickup_order/dropoff_order
4. Gọi API lọc prices để lấy giá tương ứng
```
