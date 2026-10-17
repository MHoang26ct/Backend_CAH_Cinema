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

## Phân biệt token và tương thích khi cập nhật

- **Access token** có claim `purpose=ACCESS`, `sub` là định danh đăng nhập và `userId` khớp tài khoản hiện tại. Chỉ loại này dùng trong `Authorization: Bearer ...` để truy cập API cần đăng nhập. Quyền lấy từ tài khoản trong DB, không lấy từ claim role do token khai báo.
- **Reset token** có `purpose=RESET_PASSWORD`, chỉ dùng làm `resetToken` tại `/api/v1/auth/fp-change-password` cho đúng email. Không dùng reset token để truy cập profile, booking hoặc API staff/admin.
- **Ticket QR** có `purpose=TICKET_QR`, chỉ dùng trong luồng kiểm tra vé; không thay access token. Access/reset token cũng không thay được QR.
- Token phải còn hạn; tài khoản dùng access token phải tồn tại và còn hợp lệ. Token không có purpose, purpose không đúng hoặc lẫn claim cũ `Purpose` đều bị từ chối.
- **Chuyển phiên bản:** access token cũ không có purpose không còn được chấp nhận. Client gọi `/api/v1/auth/refresh` bằng refresh token còn hợp lệ hoặc đăng nhập lại để lấy access token mới. Reset token cũ dùng `Purpose="Change password"` cũng không còn hợp lệ; khách cần thực hiện lại luồng OTP quên mật khẩu. Định dạng refresh token không thay đổi. QR hiện có với `purpose=TICKET_QR` vẫn dùng được nếu còn hạn và đáp ứng kiểm tra vé hiện hành.

## User đã đăng nhập

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/auth/change-password` | `oldPassword`, `newPassword` bắt buộc |

Endpoint đổi mật khẩu này yêu cầu JWT; các endpoint auth còn lại trong bảng Public không yêu cầu JWT.
