# Cinema Backend API Documentation

Tài liệu HTTP thủ công cho Cinema Backend. Tài liệu này được đối chiếu với các controller và DTO trong `src/main/java`.

## Nguồn tài liệu chuẩn

- **Swagger UI khi ứng dụng chạy:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Base URL triển khai:** cấu hình trong `OpenApiConfig` hoặc biến môi trường/deployment của môi trường đang dùng. Không dùng URL ngrok cố định trong tài liệu vì URL này có thể hết hiệu lực.

## Xác thực và response

- API dưới `/api/v1/public/**` và đa số `/api/v1/auth/**` không yêu cầu JWT.
- API `/api/v1/users/**`, `/api/v1/bookings/**`, `/api/v1/seats/**`, `/api/v1/comments/**`, `/api/v1/foods/**`, `/api/v1/vouchers/**` yêu cầu JWT hợp lệ.
- API `/api/v1/staff/**` yêu cầu `ROLE_STAFF` hoặc `ROLE_ADMIN`; API `/api/v1/admin/**` yêu cầu `ROLE_ADMIN`.
- Các response của đa số controller được bọc theo dạng `ApiResponse` (`code`, `message`, `data`). Riêng nhóm quản lý bài viết khuyến mãi của admin trả response body trực tiếp; xem [11. Bài viết khuyến mãi](./11-promotions.md).
- Với Swagger bearer auth, nhập JWT không kèm tiền tố `Bearer`.

## Modules

- [01. Authentication & Tài khoản](./01-auth.md)
- [02. Quản lý Phim & Thể loại](./02-movies.md)
- [03. Quản lý Rạp & Phòng chiếu](./03-cinemas.md)
- [04. Lịch chiếu](./04-showtimes.md)
- [05. Ghế, Đặt vé & Thanh toán](./05-bookings.md)
- [06. Voucher & Khuyến mãi](./06-vouchers.md)
- [07. Cấu hình hệ thống (Giá, Ngày lễ, Đồ ăn)](./07-system-config.md)
- [08. Báo cáo & Thống kê](./08-reports.md)
- [09. Hồ sơ người dùng](./09-users.md)
- [10. Bình luận phim](./10-comments.md)
- [11. Bài viết khuyến mãi](./11-promotions.md)
- [12. Nghiệp vụ Check-in vé](./12-tickets.md)
