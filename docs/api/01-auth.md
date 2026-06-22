# 1. Authentication (Xác thực & Tài khoản)

## Dành cho Public (Không yêu cầu đăng nhập)

### Đăng ký tài khoản

- **Endpoint:** `POST /api/v1/auth/register`
    
- **Request Body:**
    
    - `email` (string, **required**)
        
    - `password` (string, **required**)
        
    - `name` (string, **required**)
        
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
