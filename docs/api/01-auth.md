# 1. Authentication & tài khoản

## Public

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register/send-otp` | `email` bắt buộc, đúng định dạng email |
| `POST` | `/api/v1/auth/register` | `email`, `password`, `name`, `otp` bắt buộc; `phone` tùy chọn |
| `POST` | `/api/v1/auth/login` | `email`, `password` bắt buộc |
| `POST` | `/api/v1/auth/google` | `idToken` bắt buộc |
| `POST` | `/api/v1/auth/send-otp` | `email` bắt buộc |
| `POST` | `/api/v1/auth/verify-otp` | `email`, `otp` bắt buộc |
| `POST` | `/api/v1/auth/fp-verify-otp` | `email`, `otp` bắt buộc |
| `POST` | `/api/v1/auth/fp-change-password` | `email`, `newPassword`, `resetToken` bắt buộc |
| `POST` | `/api/v1/auth/refresh` | `refreshToken` |
| `POST` | `/api/v1/auth/logout` | `refreshToken` |

### Đăng ký bằng OTP

1. Gọi `POST /api/v1/auth/register/send-otp` với email.
2. Gọi `POST /api/v1/auth/register` với cùng email, OTP 6 chữ số, mật khẩu và tên.

OTP đăng ký tách biệt với luồng `/send-otp` và `/verify-otp`. Backend chỉ tạo tài khoản/cấp token sau khi OTP đăng ký được xác thực thành công. OTP đăng ký gồm 6 chữ số, có hiệu lực 5 phút, dùng một lần, và việc gửi lại cùng email bị giới hạn 60 giây. Phần tên miền email được chuẩn hóa không phân biệt hoa/thường cho luồng OTP đăng ký.

Ví dụ register:

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "otp": "123456"
}
```

Response thành công của đăng ký, login và Google login là `ApiResponse` chứa `accessToken`, `refreshToken` và thông tin `user`.

## User đã đăng nhập

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/auth/change-password` | `oldPassword`, `newPassword` bắt buộc |

Endpoint đổi mật khẩu này yêu cầu JWT; các endpoint auth còn lại trong bảng Public không yêu cầu JWT.
