# 1. Authentication (Xác thực & Tài khoản)

## Dành cho Public (Không yêu cầu đăng nhập)

### Đăng ký tài khoản

Đăng ký bằng email gồm hai bước. Backend chỉ tạo tài khoản và cấp token sau khi
OTP đăng ký được xác thực thành công.

#### Bước 1: Gửi OTP đăng ký

- **Endpoint:** `POST /api/v1/auth/register/send-otp`
- **Authentication:** Không yêu cầu
- **Request body:**

```json
{
  "email": "user@example.com"
}
```

- **Response `200 OK`:**

```json
{
  "code": 200,
  "message": "Gửi OTP đăng ký thành công",
  "data": null
}
```

OTP gồm 6 chữ số, có hiệu lực trong 5 phút và chỉ dùng một lần. Mỗi email chỉ
được yêu cầu gửi lại sau 60 giây. Phần tên miền email không phân biệt chữ hoa,
chữ thường khi áp dụng thời gian chờ và xác thực OTP.

#### Bước 2: Đăng ký bằng OTP

- **Endpoint:** `POST /api/v1/auth/register`
- **Authentication:** Không yêu cầu
- **Request body:**

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "otp": "123456"
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `email` | string | Có | Email đã nhận OTP đăng ký |
| `password` | string | Có | Mật khẩu tài khoản |
| `name` | string | Có | Họ và tên |
| `phone` | string | Không | Số điện thoại |
| `otp` | string | Có | Chính xác 6 chữ số |

- **Response `200 OK`:**

```json
{
  "code": 200,
  "message": "Đăng ký thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "2f8f62c0-...",
    "user": {
      "userId": 1,
      "name": "Nguyễn Văn A",
      "email": "user@example.com",
      "phone": "0901234567",
      "avatarUrl": null,
      "authProvider": "EMAIL",
      "role": "ROLE_USER",
      "totalPaid": 0,
      "totalPoint": 0,
      "rankLevel": "SILVER"
    }
  }
}
```

#### Mã lỗi đăng ký

| HTTP | `code` | Trường hợp |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | Thiếu trường bắt buộc, email sai định dạng hoặc OTP không đủ 6 chữ số |
| `400` | `OTP_INVALID` | OTP sai, hết hạn, đã dùng hoặc đã bị hủy sau 5 lần nhập sai |
| `409` | `EMAIL_ALREADY_EXISTS` | Email đã có tài khoản |
| `429` | `OTP_RATE_LIMITED` | Yêu cầu gửi lại OTP trước khi hết 60 giây |

OTP do `/send-otp` và `/verify-otp` cấp không thể dùng để đăng ký. Nếu việc tạo
tài khoản thất bại sau khi OTP đã được tiêu thụ, client cần yêu cầu OTP mới.


### Đăng nhập (Email/Password)

- **Endpoint:** `POST /api/v1/auth/login`
    
- **Request Body:**
    
    - `email` (string, **required**)
        
    - `password` (string, **required**)
        
- **Response:** `200 OK`

```json
{
  "code": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "userId": 1,
      "name": "Nguyễn Văn A",
      "email": "a@example.com",
      "phone": "0901234567",
      "avatarUrl": "https://...",
      "authProvider": "EMAIL",
      "role": "ROLE_USER",
      "totalPaid": 1200000.00,
      "totalPoint": 30,
      "rankLevel": "SILVER"
    }
  }
}
```
    

### Đăng nhập Google

- **Endpoint:** `POST /api/v1/auth/google`
    
- **Request Body:**
    
    - `idToken` (string, **required**)
        

### OTP & Quên mật khẩu

- **Gửi OTP:** `POST /api/v1/auth/send-otp`
    
    - Body: `email` (string, **required**)
        
- **Xác thực OTP:** `POST /api/v1/auth/verify-otp`
    
    - Body: `email` (string, **required**), `otp` (string)
        
- **Xác thực OTP quên mật khẩu:** `POST /api/v1/auth/fp-verify-otp`
    
    - Body: `email` (string, **required**), `otp` (string)
        
- **Đổi mật khẩu sau khi xác thực OTP:** `POST /api/v1/auth/fp-change-password`
    
    - Body: `email` (string, **required**), `newPassword` (string, **required**), `resetToken` (string, **required**)

- **Làm mới Token:** `POST /api/v1/auth/refresh`
    
    - Body: `refreshToken` (string)
        

## Yêu cầu đăng nhập (User / Admin)

### Quản lý phiên đăng nhập

- **Đăng xuất:** `POST /api/v1/auth/logout`
    
    - Body: `refreshToken` (string)
        
- **Đổi mật khẩu:** `POST /api/v1/auth/change-password`
    
    - Body: `oldPassword` (string, **required**), `newPassword` (string, **required**)
