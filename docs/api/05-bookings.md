# 5. Ghế, Đặt vé & Thanh toán

## Admin — yêu cầu `ROLE_ADMIN`

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/admin/seats/create` | Mảng không rỗng các seat: `roomId`, `row`, `col`, `seatTypeId` |
| `GET` | `/api/v1/admin/seats/rooms/{roomId}` | — |
| `DELETE` | `/api/v1/admin/seats/delete/{roomId}` | — |
| `PUT` | `/api/v1/admin/seats/replace` | `roomId`, `seats` (mảng không rỗng) |

## Staff / Admin

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/staff/bookings` | Tạo đơn tại quầy cho khách REGISTERED hoặc WALK_IN |
| `GET` | `/api/v1/staff/customers/search` | Query `email` chính xác để tìm khách hàng |
| `POST` | `/api/v1/staff/bookings/{bookingId}/confirm-payment` | `paymentRef`, `gateway` bắt buộc |
| `GET` | `/api/v1/staff/bookings/{bookingId}/cancellation-quote` | Xem báo giá hủy đơn tại quầy |
| `POST` | `/api/v1/staff/bookings/{bookingId}/cancel` | Hủy đơn tại quầy kèm xác nhận quote token |
| `POST` | `/api/v1/staff/refunds/{refundId}/claim` | Nhận xử lý hoàn tiền mặt (atomic claim) |
| `POST` | `/api/v1/staff/refunds/{refundId}/confirm-cash-payout` | Xác nhận đã chi tiền mặt cho khách |

Endpoint này yêu cầu `ROLE_STAFF` hoặc `ROLE_ADMIN`.

### Tạo booking tại quầy (`POST /api/v1/staff/bookings`)
Request body (`StaffCreateBookingRequestDTO`):
```json
{
  "customerType": "WALK_IN", // hoặc "REGISTERED"
  "customerId": null,        // bắt buộc nếu REGISTERED
  "contactName": "Nguyen Van A",
  "contactEmail": "customer@example.com",
  "contactPhone": "0901234567",
  "showtimeId": 10,
  "paymentMethod": "CASH",
  "seatIds": [101, 102],
  "voucherId": null,
  "foodItems": [{"foodId": 1, "quantity": 2}]
}
```

## Public

| Method | Endpoint | Query |
| --- | --- | --- |
| `GET` | `/api/v1/public/seats` | `showtimeId` bắt buộc |

## User đã đăng nhập

> Các API dưới đây yêu cầu JWT hợp lệ; không phải public.

| Method | Endpoint | Request / query |
| --- | --- | --- |
| `POST` | `/api/v1/seats/{seatId}/lock` | query `showtimeId` bắt buộc |
| `DELETE` | `/api/v1/seats/{seatId}/lock` | query `showtimeId` bắt buộc |
| `POST` | `/api/v1/seats/pre-lock` | `showtimeId`, `seatIds` (mảng không rỗng) |
| `POST` | `/api/v1/bookings` | `showtimeId`, `paymentMethod`, `seatIds` bắt buộc; `voucherId`, `foodItems` tùy chọn |
| `GET` | `/api/v1/bookings/{bookingId}` | Lấy trạng thái booking thuộc về user hiện tại |
| `GET` | `/api/v1/bookings/{bookingId}/cancellation-quote` | Xem báo giá hủy đơn (chính chủ) |
| `POST` | `/api/v1/bookings/{bookingId}/cancel` | Xác nhận hủy đơn với quote token (chính chủ) |
| `POST` | `/api/v1/bookings/{bookingId}/momo/pay` | `requestId` bắt buộc; `requestType` tùy chọn |
| `POST` | `/api/v1/bookings/{bookingId}/vnpay/pay` | `requestId` bắt buộc |

### Chính sách báo giá hủy (`cancellation-quote`):
- Trước giờ chiếu >= 24h: hoàn 100% tiền vé net.
- Trước giờ chiếu >= 6h: hoàn 80% tiền vé net, giữ lại 20%.
- Trước giờ chiếu >= 2h: hoàn 50% tiền vé net, giữ lại 50%.
- Dưới 2h trước giờ chiếu: không cho phép hủy (`BOOKING_CANCEL_DEADLINE_EXCEEDED`).
- Đồ ăn chưa giao (`!= FULFILLED`): hoàn 100% tiền đồ ăn net. Đồ ăn đã giao: không hoàn tiền đồ ăn.
- Response trả `quoteToken` và `totalRefundAmount`. Khi gọi `/cancel`, bắt buộc gửi ít nhất một trong `quoteToken` hoặc `expectedRefundAmount`. Backend tính lại quote dưới khóa booking; nếu gửi cả hai thì cả hai phải khớp. Thiếu xác nhận hoặc giá đã đổi trả `QUOTE_EXPIRED_OR_CHANGED`; client phải lấy lại quote. Token là dấu vân tay của báo giá, không phải token xác thực/quyền truy cập.
- `policySnapshot` được lưu nội bộ cùng booking và refund, không phải trường trong response quote.
- Booking lịch sử được đánh dấu `legacy_snapshot_status=REVIEW_REQUIRED` bị chặn quote/cancel với `BOOKING_CANNOT_BE_CANCELLED` cho đến khi đối soát có bằng chứng.
- Refund chỉ được tạo từ receipt đã xác minh khớp `PaymentConfirmation SUCCESS` của booking (gateway + transaction reference), không lấy receipt đầu tiên trong danh sách. Thiếu/mâu thuẫn chứng từ trả HTTP 409 `PAYMENT_NOT_CONFIRMED`, giữ nguyên booking/vé; cần đối soát thủ công.
- Hủy thành công trả `status=CANCELLED` và `refundStatus=REQUESTED` nếu có tiền phải hoàn. Đây **không** phải xác nhận đã hoàn tiền. Hủy có số tiền hoàn bằng 0 trả `refundId=null`, `refundStatus=null`.
- Admin hủy suất: booking `PENDING` chuyển `CANCELLED` ngay dù chưa hết hạn; draft/voucher được giải phóng, Redis seat lock chỉ nhả sau commit. Booking `PAID` giữ voucher đã dùng. Trạng thái suất, booking và nghĩa vụ refund cùng một transaction; không báo thành công khi không thể ghi nghĩa vụ hoàn tiền.

`paymentMethod` dùng enum backend (ví dụ `CASH`, `VNPAY`, `MOMO`). Chú ý: `CASH` online bị chặn với mã lỗi `CASH_PAYMENT_REQUIRES_STAFF`; MoMo trực tuyến bị chặn qua cờ tính năng `momoEnabled`.

`paymentMethod` dùng enum backend (ví dụ `CASH`, `VNPAY`, `MOMO`). Mỗi phần tử `foodItems` có `foodId` và `quantity` (tối thiểu 1).

Response tạo booking (`POST /api/v1/bookings`) trả `ApiResponse.data` với các trường:

```json
{
  "bookingId": 42,
  "status": "PENDING",
  "expiresAt": "2026-05-18T18:15:00",
  "seatSubtotal": 196000.00,
  "foodSubtotal": 73500.00,
  "discountAmount": 26950.00,
  "lateDiscountAmount": 0.00,
  "voucherDiscountAmount": 26950.00,
  "totalAmount": 242550.00
}
```

`discountAmount` là tổng của `lateDiscountAmount` và `voucherDiscountAmount`.

## Callback thanh toán — Public

| Method | Endpoint | Response |
| --- | --- | --- |
| `POST` | `/api/v1/public/momo/ipn` | `204 No Content` |
| `GET` | `/api/v1/public/vnpay/ipn` | `200 OK` với `VnpayIpnResponse` |

Các callback này dành cho cổng thanh toán gọi server-to-server, không dành cho client application gọi trực tiếp.
