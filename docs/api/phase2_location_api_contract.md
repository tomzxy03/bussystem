# API Contract: Phase 2 – Location (Provinces, Districts, Stops)

This document is intended for the **Frontend (FE) team**. All APIs follow the standard `ApiResponse<T>` wrapper.

## Base URL
All paths prefixed with `/api/v1`

## Standard Response Format
```json
{
  "success": true,
  "data": { ... },
  "message": "Thành công",
  "errorCode": null,
  "code": 200,
  "timestamp": "2026-04-25T10:00:00+07:00"
}
```

---

## 🗺️ Province APIs (Public)

### 1. Danh sách tỉnh/thành phố
- **Endpoint**: `GET /provinces`
- **Auth**: Public
- **Query Params**:
  - `keyword` (optional, string): tìm theo tên (case-insensitive)
- **Caching**: Danh sách đầy đủ được cache Redis 24h (`busozy:{env}:location:provinces`). Keyword search không cache.
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "01",
      "name": "Hà Nội",
      "fullName": "Thành phố Hà Nội",
      "lat": 21.0245,
      "lng": 105.8412
    }
  ],
  "message": "Thành công",
  "errorCode": null,
  "code": 200,
  "timestamp": "..."
}
```

---

### 2. Danh sách quận/huyện theo tỉnh
- **Endpoint**: `GET /provinces/{id}/districts`
- **Auth**: Public
- **Path Variable**: `id` – Province ID
- **Caching**: Cached per province 24h (`busozy:{env}:location:districts:{provinceId}`)
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "code": "001",
      "name": "Ba Đình",
      "fullName": "Quận Ba Đình",
      "provinceId": 1
    }
  ],
  ...
}
```
- **Error Response**: `LOC_001` nếu province không tồn tại.

---

## 🚏 Stop APIs (Public)

### 3. Tìm kiếm điểm dừng
- **Endpoint**: `GET /stops`
- **Auth**: Public
- **Query Params**:

| Param | Type | Description |
|---|---|---|
| `provinceId` | Long | Lọc theo tỉnh |
| `districtId` | Long | Lọc theo quận |
| `type` | Enum | `STATION`, `OFFICE`, `ROAD_POINT`, `CUSTOM` |
| `isMajor` | Boolean | Chỉ lấy điểm chính |
| `keyword` | String | Tìm theo tên hoặc địa chỉ |
| `page` | Int | Trang (default: 0) |
| `size` | Int | Số lượng/trang (default: 20, max: 100) |
| `sort` | String | Ví dụ: `name,asc` hoặc `id,desc` |

- **Success Response (200 OK)** – paginated:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "HN_BX_GV",
        "name": "Bến xe Giáp Bát",
        "type": "STATION",
        "provinceName": "Hà Nội",
        "districtName": "Hoàng Mai",
        "address": "Số 9, Giải Phóng, Hoàng Mai",
        "lat": 20.9905,
        "lng": 105.8411,
        "isMajor": true
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "page": 0,
    "size": 20
  },
  ...
}
```

---

### 4. Chi tiết điểm dừng
- **Endpoint**: `GET /stops/{id}`
- **Auth**: Public
- **Caching**: Cached 1h per stop (`busozy:{env}:location:stop:{id}`)
- **Success Response (200 OK)**: Single `StopResDTO` object (same structure as content item above)
- **Error**: `LOC_006` nếu không tìm thấy

---

## 🔐 Admin Stops APIs

> **Yêu cầu xác thực**: `Authorization: Bearer <access_token>`

### 5. Tạo điểm dừng (Create Stop)
- **Endpoint**: `POST /admin/stops`
- **Auth**: Bearer Token (ADMIN)
- **Headers**:
  - `Idempotency-Key` (optional): Chống tạo trùng khi retry
- **Request Body**:
```json
{
  "code": "HN_BX_MY_DINH",
  "name": "Bến xe Mỹ Đình",
  "type": "STATION",
  "provinceId": 1,
  "districtId": 10,
  "address": "20 Phạm Hùng, Nam Từ Liêm",
  "latitude": 21.0284,
  "longitude": 105.7827,
  "isMajor": true
}
```
- **Validation**:
  - `code`: `[A-Z0-9_-]{3,50}` (optional)
  - `name`: 3-100 ký tự, bắt buộc
  - `type`: bắt buộc, một trong `STATION`, `OFFICE`, `ROAD_POINT`, `CUSTOM`
  - `provinceId`, `districtId`: bắt buộc, districtId phải thuộc province
  - `latitude`: -90 đến 90, bắt buộc
  - `longitude`: -180 đến 180, bắt buộc
  - `address`: tối đa 500 ký tự
- **Success Response (201 Created)**: `StopResDTO`

---

### 6. Cập nhật điểm dừng (Update Stop)
- **Endpoint**: `PUT /admin/stops/{id}`
- **Auth**: Bearer Token (ADMIN)
- **Request Body**: Giống Create (đầy đủ)
- **Success Response (200 OK)**: Updated `StopResDTO`

---

### 7. Xóa mềm điểm dừng (Soft Delete)
- **Endpoint**: `DELETE /admin/stops/{id}`
- **Auth**: Bearer Token (ADMIN)
- **Effect**: Set `deleted_at` và `is_active = false`. Stop sẽ không hiển thị ở public APIs.
- **Cache**: Tự động xóa cache `location:stop:{id}` và danh sách districts
- **Success Response (200 OK)**: `{ "success": true, "message": "Xóa thành công", ... }`

---

## Error Codes (Phase 2)

| Code | Mô tả |
|---|---|
| `LOC_001` | Tỉnh/thành phố không tồn tại |
| `LOC_002` | Quận/huyện không tồn tại |
| `LOC_003` | Quận không thuộc tỉnh được chọn |
| `LOC_004` | Mã điểm dừng đã tồn tại |
| `LOC_005` | Tọa độ không hợp lệ |
| `LOC_006` | Điểm dừng không tồn tại |

---

## Seed Data Note
Dữ liệu tỉnh/huyện được import từ [vietnamese-provinces-database](https://github.com/thanglequoc/vietnamese-provinces-database).
Script seed được deploy qua Flyway migration `V4__seed_locations.sql` (chạy bởi DevOps khi deploy lần đầu).
