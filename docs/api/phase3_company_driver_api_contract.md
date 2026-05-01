# API Contract: Phase 3 – Company & Driver

Document for FE team. All responses use standard `ApiResponse<T>` wrapper.

## Base URL: `/api/v1`

---

## 🏢 Company APIs

### 1. Danh sách công ty (Public)
- **Endpoint**: `GET /companies`
- **Auth**: Public
- **Query Params**:

| Param | Type | Default | Description |
|---|---|---|---|
| `keyword` | String | - | Tìm theo tên (case-insensitive) |
| `page` | Int | 0 | Trang |
| `size` | Int | 20 | Tối đa 100 |
| `sort` | String | `name,asc` | Ví dụ: `name,desc` |

- **Caching**: Company detail cache 4h, list không cache (always fresh from DB + paginated)
- **Success Response (200)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Phương Trang",
        "taxCode": "0301234567",
        "phone": "0918123456",
        "address": "272 Đề Thám, Q.1, TP.HCM",
        "isActive": true,
        "createdAt": "2026-04-25T09:00:00+07:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "page": 0,
    "size": 20
  }
}
```

---

### 2. Chi tiết công ty (Public)
- **Endpoint**: `GET /companies/{id}`
- **Auth**: Public
- **Caching**: Redis TTL 4h (`busozy:{env}:company:{id}`)
- **Success Response (200)**: Single `CompanyResDTO`
- **Error**: `COMP_001` nếu không tìm thấy

---

### 3. Tạo công ty (Platform Admin)
- **Endpoint**: `POST /admin/companies`
- **Auth**: Bearer Token (`PLATFORM_ADMIN`)
- **Request Body**:
```json
{
  "name": "Phương Trang",
  "taxCode": "0301234567",
  "phone": "0918123456",
  "address": "272 Đề Thám, Q.1, TP.HCM"
}
```
- **Validation**:
  - `name`: 3-100 ký tự, bắt buộc
  - `taxCode`: regex `^[0-9]{10}(-[0-9]{3})?$` (10 số hoặc 10-3 cho chi nhánh), optional
  - `phone`: regex VN `^0[35789][0-9]{8}$`, bắt buộc
  - `address`: tối đa 500 ký tự
- **Success Response (201)**: `CompanyResDTO`
- **Error**: `COMP_002` nếu taxCode đã tồn tại

---

### 4. Cập nhật công ty (Platform Admin)
- **Endpoint**: `PUT /admin/companies/{id}`
- **Auth**: Bearer Token (`PLATFORM_ADMIN`)
- **Request Body**: Giống Create
- **Success Response (200)**: Updated `CompanyResDTO`

---

### 5. Xóa mềm công ty (Platform Admin)
- **Endpoint**: `DELETE /admin/companies/{id}`
- **Auth**: Bearer Token (`PLATFORM_ADMIN`)
- **Business Rule**: ❌ Không thể xóa nếu công ty còn xe đang hoạt động (`COMP_004`)
- **Effect**: Set `deleted_at`, `is_active = false`. Tự động xóa cache
- **Success Response (200)**: `{ "success": true, "message": "Xóa thành công" }`

---

## 🚗 Driver Ownership

- Driver hiện là dữ liệu thuộc `company`.
- FE không dùng admin driver APIs nữa.
- Vendor quản lý tài xế qua nhóm `/api/v1/vendor/drivers`.
- Platform admin chỉ quản lý `company`, không CRUD `driver` hằng ngày.

---

## Error Codes (Phase 3)

| Code | Mô tả |
|---|---|
| `COMP_001` | Công ty không tồn tại |
| `COMP_002` | Mã số thuế đã được sử dụng |
| `COMP_003` | Công ty chưa được kích hoạt |
| `COMP_004` | Không thể xóa công ty đang có xe hoạt động |
| `DRV_001` | Tài xế không tồn tại |
| `DRV_002` | Số GPLX đã tồn tại trong công ty |
| `DRV_003` | Số điện thoại đã tồn tại trong công ty |
| `DRV_004` | Công ty của tài xế không tồn tại |

---

## Notes for FE
- **Driver status enum**: `ACTIVE`, `INACTIVE`, `BLOCKED` (shared with User)
- **Nested company**: `DriverResDTO.company` chỉ gồm `{ id, name }` — không trả về full company
- Contract chi tiết cho vendor driver APIs nằm ở [vendor_api_contract.md](/home/tomzxy/projects/bussystem/docs/api/vendor_api_contract.md:1)
