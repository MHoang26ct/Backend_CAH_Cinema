# 6. Voucher & Khuyến mãi (Vouchers)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

- **Danh sách:** `GET /api/v1/admin/vouchers` — query `page`, `size`, `sort`; mặc định size 20, sort `voucherId,desc`. `data` là Slice phân trang.
- **Chi tiết:** `GET /api/v1/admin/vouchers/{voucherId}`.
- **Tạo:** `POST /api/v1/admin/vouchers/create`.
- **Cập nhật:** `POST /api/v1/admin/vouchers/update` — gửi đủ các trường bắt buộc, không phải cập nhật từng phần.
- **Xóa:** `DELETE /api/v1/admin/vouchers/{voucherId}` — xóa mềm, đặt `isActive=false`, `isDeleted=true`.

### Request tạo/cập nhật

| Trường | Tạo | Cập nhật | Ràng buộc |
|---|---|---|---|
| `voucherId` | Không gửi | Bắt buộc | ID voucher cần sửa |
| `code` | Bắt buộc | Bắt buộc | Không trống, không trùng mã khác |
| `type` | Bắt buộc | Bắt buộc | FIXED_AMOUNT hoặc PERCENT |
| `value` | Bắt buộc | Bắt buộc | > 0; PERCENT không vượt 100 |
| `maxDiscount` | Tùy loại | Tùy loại | PERCENT: bắt buộc > 0; FIXED_AMOUNT: null hoặc 0 |
| `minOrderValue` | DTO cho phép bỏ qua | Bắt buộc | >= 0; nên gửi 0 khi không yêu cầu tối thiểu, service áp dụng voucher cần giá trị này |
| `quantity` | Bắt buộc | Bắt buộc | > 0 |
| `startAt` | Bắt buộc | Bắt buộc | ISO local date-time |
| `expiredAt` | Bắt buộc | Bắt buộc | Phải lớn hơn `startAt` |
| `isActive` | Server đặt true | Bắt buộc | Trạng thái hoạt động |
| `isDeleted` | Server đặt false | Bắt buộc trong DTO | Service update hiện không cập nhật trường này; dùng DELETE để xóa |

Server khởi tạo `usedCount=0`; client không tự gửi số lượt đã dùng. Tạo/cập nhật/chi tiết trả HTTP 200 với `data` là VoucherResponseDTO. Xóa trả HTTP 200 với `data=null`.

## Dành cho người dùng đã đăng nhập

- **Danh sách voucher khả dụng:** `GET /api/v1/vouchers` — không phải danh sách voucher được cấp riêng theo tài khoản.
- Chỉ trả voucher đang hoạt động, chưa xóa, còn lượt và trong khoảng hiệu lực `[startAt, expiredAt)`. Tại đúng `expiredAt` đã hết hiệu lực.
- Đủ điều kiện xuất hiện trong danh sách chưa có nghĩa áp dụng được cho mọi đơn: còn kiểm tra `minOrderValue` và không kết hợp giảm giá vào xem muộn.

```json
{
  "code": 200,
  "message": "Lấy danh sách voucher thành công",
  "data": [
    {
      "voucherId": 8,
      "code": "SUMMER10",
      "type": "PERCENT",
      "value": 10.00,
      "maxDiscount": 50000.00,
      "minOrderValue": 200000.00,
      "quantity": 100,
      "usedCount": 23,
      "startAt": "2026-05-01T00:00:00",
      "expiredAt": "2026-06-01T00:00:00",
      "isActive": true,
      "isDeleted": false
    }
  ]
}
```

### Áp dụng cho booking

Gửi `voucherId` khi [tạo booking](05-bookings.md). Mỗi đơn dùng tối đa một voucher; lượt dùng được giữ ngay khi tạo đơn, không đợi thanh toán. Khi xử lý booking hết hạn, hệ thống trả lượt voucher. Giảm phần trăm làm tròn 2 chữ số HALF_UP, áp dụng mức giảm tối đa; mọi khoản giảm không vượt subtotal. Subtotal xét voucher là tiền ghế + đồ ăn sau giảm theo hạng.

Voucher hết hạn/hết lượt, không đạt mức tối thiểu hoặc dữ liệu cấu hình sai trả 400 `VALIDATION_FAILED`; không tìm thấy ID trả 404 `RESOURCE_NOT_FOUND`; mã trùng trả 400 `DUPLICATE_RESOURCE`; gửi voucher với giảm muộn trả 400 `DISCOUNT_NOT_COMBINABLE`.
