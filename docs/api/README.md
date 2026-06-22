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

- **Server:** `https://stuffy-astrology-collar.ngrok-free.dev`
- **Security:** Bearer Auth (JWT Token) - Nhập token vào Header mà không cần tiền tố `Bearer`.
