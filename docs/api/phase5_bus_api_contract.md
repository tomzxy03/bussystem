# API Contract: Phase 5 – Bus & Seat Layout

Document for FE team. All responses use standard `ApiResponse<T>` wrapper.

## Base URL: `/api/v1`

---

## 🚌 Bus Types & Seat Layouts

### 1. Danh sách loại xe active (Public)
- **Endpoint**: `GET /bus-types`
- **Auth**: Public
- **Caching**: Redis TTL **24h** (`busozy:{env}:bus-types`)
- **Success Response (200)**: Array of `BusTypeResDTO`
```json
{
  "data": [
    {
      "id": 1,
      "code": "SLEEPER_40",
      "name": "Giường nằm 40 chỗ",
      "description": "Xe giường nằm VIP",
      "amenities": {
        "wifi": true,
        "ac": true,
        "water": true
      },
      "basePricePerKm": 1500.00,
      "isActive": true
    }
  ]
}
```

### 2. Chi tiết loại xe (Public)
- **Endpoint**: `GET /bus-types/{id}`
- **Auth**: Public
- **Success Response (200)**: Single `BusTypeResDTO`

### 3. Sơ đồ ghế của một xe cụ thể (Public)
- **Endpoint**: `GET /buses/{id}/seats`
- **Auth**: Public
- **Caching**: Redis TTL **2h** (`busozy:{env}:bus:{id}:seats`)
- **Description**: Sử dụng để hiển thị UI chọn ghế.
- **Success Response (200)**: Array of `SeatResDTO` (sắp xếp theo `rowNum` và `colNum`)
```json
{
  "data": [
    {
      "id": 100,
      "seatNumber": "1A",
      "seatType": "STANDARD",
      "rowNum": 1,
      "colNum": 1,
      "priceMultiplier": 1.00,
      "isActive": true
    },
    {
      "id": 101,
      "seatNumber": "1B",
      "seatType": "VIP",
      "rowNum": 1,
      "colNum": 2,
      "priceMultiplier": 1.20,
      "isActive": true
    }
  ]
}
```

---

## 🔐 Shared Admin APIs

`PLATFORM_ADMIN` chỉ giữ các tài nguyên shared/global như `bus type` và `seat layout`.

### 4. Tạo loại xe mới (Platform Admin)
- **Endpoint**: `POST /admin/bus-types`
- **Request Body**:
```json
{
  "code": "SLEEPER_40",
  "name": "Giường nằm 40 chỗ",
  "amenities": {
    "wifi": true,
    "ac": true
  },
  "basePricePerKm": 1500.00
}
```
- **Validation**:
  - `code`: unique, chữ hoa/số/gạch dưới (`^[A-Z0-9_]{3,30}$`)
  - `basePricePerKm`: >= 0

### 5. Tạo sơ đồ ghế (Platform Admin)
- **Endpoint**: `POST /admin/seat-layouts`
- **Request Parameters**:
  - `busTypeId`: Long
  - `name`: String (Tên sơ đồ)
- **Request Body (JSONB layout data)**:
```json
{
  "rows": 10,
  "cols": 4,
  "seats": [
    {"row": 1, "col": 1, "type": "standard"},
    {"row": 1, "col": 2, "type": "vip"}
  ]
}
```
- **Validation**: Body bắt buộc có mảng `seats` (tự động tính `totalSeats` dựa trên số phần tử).

## Bus Ownership

- `bus` là dữ liệu thuộc `company`.
- FE không dùng `/api/v1/admin/buses` nữa.
- Vendor quản lý xe qua `/api/v1/vendor/buses`.
- Vendor tạo/cập nhật bus vẫn dùng `BusReqDTO`; backend tự ép `companyId` theo vendor đang đăng nhập.

---

## Error Codes (Phase 5)

| Code | Mô tả |
|---|---|
| `BUS_001` | Loại xe không tồn tại |
| `BUS_002` | Không thể xóa loại xe đang có xe hoạt động |
| `BUS_003` | Biển số xe đã tồn tại |
| `BUS_004` | Dữ liệu sơ đồ ghế không hợp lệ |
| `BUS_005` | Xe không tồn tại |
| `BUS_006` | Sơ đồ ghế không tồn tại |
| `SEAT_001` | Mã ghế trùng trong cùng xe |
| `SEAT_002` | Sơ đồ ghế không thuộc loại xe đã chọn |
| `SEAT_003` | Hệ số giá nằm ngoài khoảng 0.1 - 5.0 |

---

## SeatType Enum
- `STANDARD`
- `VIP`
- `SLEEPER`
- `EXTRA`
- `BED`
