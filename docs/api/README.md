# Cinema Backend API Documentation

Chào mừng đến với tài liệu API của hệ thống quản lý rạp chiếu phim (Cinema Backend).

Dưới đây là danh sách các module API đã được phân loại theo chức năng. Bạn có thể nhấn vào từng module để xem chi tiết.

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

---

## Thông tin chung

- **Base URL:** dùng địa chỉ môi trường đang chạy; mặc định local là `http://localhost:8080`. Địa chỉ tunnel không cố định.
- **Xác thực HTTP:** gửi header `Authorization: Bearer <access_token>`. Trong ô Authorize của Swagger dùng security scheme HTTP Bearer, chỉ nhập token vì Swagger tự thêm tiền tố.
- **Phân quyền:** `/api/v1/public/**` không yêu cầu đăng nhập; các API booking, giữ ghế, hồ sơ, đồ ăn, voucher và bình luận của người dùng yêu cầu token hợp lệ. `/api/v1/staff/**` dành cho STAFF/ADMIN; `/api/v1/admin/**` dành cho ADMIN. Quyền sở hữu tài nguyên vẫn được kiểm tra riêng.
- **Thời gian:** ngày dùng `yyyy-MM-dd`; date-time dùng ISO local, ví dụ `2026-09-25T18:00:00`, không có offset. Các instance cần thống nhất múi giờ.
- **Ví dụ:** ID, ngày và số tiền chỉ minh họa; dùng dữ liệu thực còn hiệu lực khi gọi API. Những ví dụ chỉ có `code` và `data` đã lược bỏ `message`.
- **Nghiệp vụ và giới hạn đã đối chiếu:** xem [SRS](../../SRS.md), đặc biệt mục 7.3. Tài liệu mô tả code hiện tại, không xác nhận tích hợp production đã được kiểm thử.
