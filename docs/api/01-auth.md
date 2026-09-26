# 1. Authentication (Xác thực & Tài khoản)

## Dành cho Public (Không yêu cầu đăng nhập)

### Đăng ký tài khoản

1. Gọi `POST /api/v1/auth/register/send-otp` với body `{"email":"user@example.com"}`.
   Mã đăng ký gồm 6 chữ số, có hiệu lực 5 phút. Gửi lại sau ít nhất 60 giây;
   mã mới thay thế mã cũ. Email đã có tài khoản trả `409 EMAIL_ALREADY_EXISTS`.
2. Gọi `/register` với thông tin tài khoản và `otp` nhận được. Chỉ khi mã đúng
   mới tạo tài khoản và trả access token, refresh token, user.

OTP gắn với email nhận mã (tên miền không phân biệt chữ hoa/thường), chỉ dùng
một lần và bị hủy sau 5 lần nhập sai. Giới hạn gửi lại cũng dùng cùng địa chỉ đã chuẩn hóa.
Mã sai, hết hạn hoặc đã dùng trả `400 OTP_INVALID`; thiếu/sai định dạng trả
`400 VALIDATION_FAILED`; gửi quá nhanh trả `429 OTP_RATE_LIMITED`.
OTP của `/send-otp`, `/verify-otp` và luồng quên mật khẩu không dùng để đăng ký.
Frontend phải cập nhật luồng này: `/register` hiện bắt buộc có `otp`.
Nếu tạo tài khoản thất bại sau khi mã đã được tiêu thụ, cần gửi lại OTP.

- **Endpoint:** `POST /api/v1/auth/register`
    
- **Request Body:**
    
    - `email` (string, **required**)
        
    - `password` (string, **required**)
        
    - `name` (string, **required**)
        
    - `otp` (string, **required**, 6 chữ số)

    - `phone` (string)
        
- **Response:** `200 OK` (object)
    

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
