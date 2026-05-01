# API Contract: Phase 0 & 1 (Infrastructure & Auth)

This document contains the API contracts for the completed Phase 0 & Phase 1. It is intended for the FE team to consume.

## Base URL
All API paths are prefixed with `/api/v1`

## Standard API Response Format
All APIs return the following JSON structure:
```json
{
  "success": true,
  "data": { ... },
  "message": "Thành công",
  "errorCode": null,
  "code": 200,
  "timestamp": "2026-04-19T12:45:00+07:00"
}
```
If an error occurs, `success` will be `false`, `data` will be `null` (or validation errors map), and `errorCode` will be populated.

---

## 🔒 Authentication APIs

## Portal / Role Mapping
- `CUSTOMER`: trang đặt vé của khách
- `VENDOR`: trang quản trị của nhà xe
- `PLATFORM_ADMIN`: trang admin của platform

FE nên route user theo `data.role` ngay sau login/register.

### 1. Đăng ký tài khoản (Register)
- **Endpoint**: `POST /auth/register`
- **Security**: Public
- **Headers**:
  - `Idempotency-Key` (optional, string UUID): Used to prevent duplicate registration requests (TTL 24h).
- **Request Body**:
```json
{
  "username": "tester01",
  "email": "test@busozy.vn",
  "password": "Password123",
  "fullName": "Nguyen Van A",
  "phone": "0912345678"
}
```
- **Validation Rules**:
  - `username`: 3-50 chars, alphanumeric + underscore.
  - `password`: min 8 chars, at least 1 uppercase, 1 lowercase, 1 number.
  - `phone`: VN format (`^0[35789][0-9]{8}$`).
- **Success Response (201 Created)**: Returns `AuthResDTO` (see below).

### 1.1. Đăng ký tài khoản vendor
- **Endpoint**: `POST /vendor/register`
- **Security**: Public
- **Headers**:
  - `Idempotency-Key` (optional)
- **Request Body**:
```json
{
  "account": {
    "username": "nhaxe_abc",
    "email": "owner@abc.vn",
    "password": "Password123",
    "fullName": "Nguyen Van Vendor",
    "phone": "0912345678"
  },
  "companyName": "Nha Xe ABC",
  "taxCode": "0123456789",
  "companyPhone": "0988888888",
  "address": "123 Ben Xe Mien Dong"
}
```
- **Success Response**: Trả về `AuthResDTO` với `role = VENDOR`, `userType = VENDOR`, `companyId != null`

---

### 2. Đăng nhập (Login)
- **Endpoint**: `POST /auth/login`
- **Security**: Public
- **Request Body**:
```json
{
  "credential": "tester01", // can be username OR email
  "password": "Password123"
}
```
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "ey...",
    "refreshToken": "ey...",
    "expiresIn": 900000,
    "userId": 12,
    "username": "tester01",
    "role": "CUSTOMER",
    "userType": "CUSTOMER",
    "companyId": null
  },
  "message": "Đăng nhập thành công",
  "errorCode": null,
  "code": 200,
  "timestamp": "2026-04-19T12:46:00+07:00"
}
```
`AuthResDTO`:
- `accessToken`
- `refreshToken`
- `expiresIn`: TTL access token theo milliseconds
- `userId`
- `username`
- `role`: `CUSTOMER | VENDOR | PLATFORM_ADMIN`
- `userType`: `CUSTOMER | VENDOR | PLATFORM_ADMIN`
- `companyId`: `null` với customer/platform admin, có giá trị với vendor

---

### 3. Refresh Token
- **Endpoint**: `POST /auth/refresh`
- **Security**: Public
- **Headers**:
  - `Refresh-Token` (required): The refresh token string from login response.
- **Request Body**: none.
- **Success Response (200 OK)**: Returns new `AuthResDTO` (access & refresh tokens are rotated).

---

### 4. Đăng xuất (Logout)
- **Endpoint**: `POST /auth/logout`
- **Security**: Requires Bearer Token
- **Request Body**: none.
- **Success Response (200 OK)**: Standard success boolean. Current access token will be blacklisted.

---

## 👤 User Profile APIs

### 5. Lấy thông tin cá nhân (Get Me)
- **Endpoint**: `GET /auth/me`
- **Security**: Requires Bearer Token
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "tester01",
    "email": "test@busozy.vn",
    "fullName": "Nguyen Van A",
    "phone": "0912345678",
    "gender": "MALE", // MALE, FEMALE, OTHER
    "role": "CUSTOMER",
    "userType": "CUSTOMER",
    "status": "ACTIVE", // ACTIVE, INACTIVE, BLOCKED
    "companyId": null,
    "companyName": null,
    "createdAt": "2026-04-19T12:45:00+07:00"
  },
  "message": "Thành công",
  "errorCode": null,
  "code": 200,
  "timestamp": "2026-04-19T12:48:00+07:00"
}
```

---

### 6. Cập nhật thông tin cá nhân (Update Me)
- **Endpoint**: `PUT /auth/me`
- **Security**: Requires Bearer Token
- **Request Body**:
```json
{
  "fullName": "Nguyen Van B",
  "phone": "0987654321",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "address": "123 Street",
  "avatarUrl": "https://..."
}
```
- **Success Response (200 OK)**: Returns updated `UserResDTO` object.

---

## Error Codes
The API uses predefined `errorCode` values for explicit error handling on the frontend:

- `AUTH_001`: Người dùng không tồn tại
- `AUTH_002`: Thông tin đăng nhập không hợp lệ
- `AUTH_003`: Email đã được sử dụng
- `AUTH_004`: Tên đăng nhập đã được sử dụng
- `AUTH_005`: Số điện thoại đã được sử dụng
- `AUTH_006`: Tài khoản chưa được kích hoạt hoặc đã bị khóa
- `AUTH_007`: Token không hợp lệ
- `AUTH_008`: Token đã hết hạn
- `GEN_002`: Dữ liệu đầu vào không hợp lệ (check `data` object for validation messages)
- `GEN_003`: Không có quyền truy cập
- `GEN_500`: Lỗi hệ thống nội bộ
