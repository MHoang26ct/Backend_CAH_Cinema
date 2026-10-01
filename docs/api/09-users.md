# 9. Hồ sơ người dùng

Cả hai endpoint yêu cầu JWT hợp lệ.

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/api/v1/users/me` | Profile người dùng hiện tại và tối đa 5 hóa đơn/booking gần nhất theo dữ liệu backend |
| `PATCH` | `/api/v1/users/me` | Cập nhật một phần profile |

## Cập nhật profile

Các field request đều tùy chọn; chỉ field được gửi mới được cập nhật:

| Field | Ràng buộc |
| --- | --- |
| `name` | 2–100 ký tự nếu có |
| `email` | đúng định dạng email nếu có |
| `phone` | 9–11 ký tự nếu có |
| `avatarUrl` | không có ràng buộc validation ở DTO |

Cả hai endpoint trả `ApiResponse`. Response profile gồm thông tin user và danh sách `recentInvoices`; mỗi hóa đơn có booking, suất chiếu, ghế và đồ ăn liên quan theo dữ liệu hiện có.
