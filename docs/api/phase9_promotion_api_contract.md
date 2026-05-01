# API Contract: Phase 9 – Promotion (Khuyến Mãi)

## Base URL: `/api/v1`

---

## 🏷️ Public / User Endpoints

### 1. Kiểm tra & Áp dụng mã khuyến mãi (Validate)
- **Endpoint**: `POST /promotions/validate`
- **Auth**: USER (`Bearer <token>`)
- **Mô tả**: Dùng để check mã hợp lệ (còn hạn, đủ điều kiện tuyến, giá trị đơn) và tính số tiền được giảm.
> **LƯU Ý**: Gọi API này **TRƯỚC** khi gọi khởi tạo thanh toán (`POST /payments/initiate`). KHÔNG cache kết quả API này.

- **Request Body**:
```json
{
  "promotionCode": "SUMMER2024",
  "routeId": 15,
  "orderValue": 500000.00
}
```

- **Success Response (200)**:
```json
{
  "data": {
    "isValid": true,
    "promotionCode": "SUMMER2024",
    "promotionName": "Giảm 10% Hè Sôi Động",
    "discountAmount": 50000.00,
    "finalPrice": 450000.00,
    "message": "Áp dụng thành công"
  }
}
```

- **Invalid Code Response (200)**:
> Không trả về lỗi HTTP 400. FE cần xử lý dựa trên `isValid=false` và show `message`.
```json
{
  "data": {
    "isValid": false,
    "promotionCode": null,
    "promotionName": null,
    "discountAmount": null,
    "finalPrice": null,
    "message": "Mã khuyến mãi đã hết lượt sử dụng"
  }
}
```

## 🛠️ Admin Endpoints

### 2. Danh sách khuyến mãi (Admin)
- **Endpoint**: `GET /admin/promotions`
- **Auth**: ADMIN (`Bearer <token>`)
- **Query**: `?page=0&size=20` (sort mặc định `createdAt,desc`)
- **Response**:
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "code": "SUMMER2024",
        "name": "Hè Sôi Động 2024",
        "description": "Giảm giá 50k",
        "discountType": "FIXED",
        "discountValue": 50000.00,
        "minOrderValue": 250000.00,
        "maxDiscount": null,
        "validFrom": "2024-06-01T00:00:00Z",
        "validTo": "2024-08-31T23:59:59Z",
        "usageLimit": 1000,
        "usedCount": 42,
        "perUserLimit": 1,
        "isActive": true,
        "createdAt": "2024-05-25T10:00:00Z"
      }
    ],
    ... (pagination fields)
  }
}
```

### 3. Chi tiết khuyến mãi (Admin)
- **Endpoint**: `GET /admin/promotions/{id}`
- **Auth**: ADMIN

### 4. Tạo khuyến mãi mới (Admin)
- **Endpoint**: `POST /admin/promotions`
- **Auth**: ADMIN
- **Request Body**:
```json
{
  "code": "TESTCODE",
  "name": "Mã Test",
  "description": "...",
  "discountType": "PERCENTAGE",  // "PERCENTAGE" | "FIXED"
  "discountValue": 10.00,        // 10%
  "minOrderValue": 100000.00,
  "maxDiscount": 30000.00,       // Giảm tối đa 30k
  "validFrom": "2024-06-01T00:00:00Z",
  "validTo": "2024-12-31T23:59:59Z",
  "usageLimit": 500,             // Tổng số lượt = 500 (gửi null nếu không giới hạn)
  "perUserLimit": 2,             // 1 user dùng tối đa 2 lần
  "routeIds": [1, 2]             // Gửi mảng rỗng [] nếu áp dụng cho tất cả
}
```

### 5. Cập nhật khuyến mãi (Admin)
- **Endpoint**: `PUT /admin/promotions/{id}`
- **Auth**: ADMIN
- **Request Body**: (giống lúc CREATE)

### 6. Xóa khuyến mãi (Admin / Soft Delete)
- **Endpoint**: `DELETE /admin/promotions/{id}`
- **Auth**: ADMIN

---

## 🛑 Error Codes bổ sung (Phase 9)

| Code | Mô tả (FE dùng để tra cứu hoặc map locale) |
|---|---|
| `PROMO_001` | Mã khuyến mãi không tồn tại hoặc không hoạt động |
| `PROMO_002` | Mã khuyến mãi đã hết hạn |
| `PROMO_003` | Mã đã hết lượt sử dụng |
| `PROMO_004` | Bạn đã dùng mã này tối đa số lần cho phép |
| `PROMO_005` | Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã |
| `PROMO_006` | Mã không áp dụng cho tuyến đường này |
| `PROMO_007` | Mã đã được áp dụng cho đơn hàng này |

---

## 🔀 Workflow khi User Book Vé

1. **User chọn tuyến + ghế → FE tính `basePrice` tạm thời**
2. **User nhập mã KM → FE gọi `POST /promotions/validate`**
   - Truyền `{ promotionCode, routeId, orderValue: basePrice }`
3. **Nếu `isValid == true`:**
   - FE hiển thị `discountAmount` (tiền giảm)
   - Lưu trữ `promotionId` vào state (hoặc backend tự map khi truyền `promotionCode` lúc Tạo Booking).
   - FE hiển thị Tổng Tiền = `finalPrice` từ response.
4. **Nếu `isValid == false`:**
   - FE hiển thị lỗi: API trả dòng lý do trong `message`.
5. **Gọi API Tạo Booking (`POST /bookings`)**
   - Phase 9 Backend sẽ được cắm thêm `promotionCode` (nếu cần ở Phase sau) để map tự động.
   *(Hiện tại, Phase 7 Bookings đang chỉ lưu `promotion_id`. Tùy chỉnh backend sẽ cần truyền `promotionCode` lên khi Booking).*
6. **Thanh toán thành công:**
   - Backend tự động tăng `usedCount` của mã KM.
