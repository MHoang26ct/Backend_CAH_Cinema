# 12. Quản lý và Check-in vé

## Người dùng / UI hiển thị vé

| Method | Endpoint | Yêu cầu |
| --- | --- | --- |
| `GET` | `/api/v1/bookings/{bookingId}` | Yêu cầu JWT (chính chủ). Khi booking ở trạng thái `PAID` hoặc `CHECKED_IN`, response trả về mảng `tickets` chứa danh sách vé kèm `checkinCode` và `qrCode`. |
| `GET` | `/api/v1/bookings/{bookingId}/tickets` | Yêu cầu JWT (chính chủ hoặc STAFF/ADMIN). Lấy danh sách chi tiết vé của booking. |

Response của danh sách vé:

```json
{
  "code": 200,
  "message": "Lấy danh sách vé thành công",
  "data": [
    {
      "ticketId": 12,
      "bookingId": 45,
      "showtimeId": 100,
      "seatId": 204,
      "seatName": "A8",
      "price": 85000.00,
      "checkinCode": "TK-9M4K-2P7Q",
      "qrCode": "TK-9M4K-2P7Q",
      "status": "VALID",
      "isCheckedIn": false
    }
  ]
}
```

*Lưu ý:* `qrCode` trùng với `checkinCode` (chuỗi rút gọn 14 ký tự). Frontend có thể ném trực tiếp giá trị này vào bất kỳ thư viện hiển thị QR code nào (ví dụ `qrcode.react`) để vẽ mã QR trên màn hình; mã chỉ cần QR Version 1 (21x21 ô vuông) giúp người dùng dễ quét ngay cả khi màn hình tối.

## Staff / Admin Check-in

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/staff/tickets/check-in` | `{ "qrToken": "TK-9M4K-2P7Q" }` |

Endpoint yêu cầu `ROLE_STAFF` hoặc `ROLE_ADMIN`. `qrToken` hỗ trợ cả mã rút gọn `checkinCode` (VD: `TK-9M4K-2P7Q`) và token JWT cũ (tương thích ngược). Nhân viên có thể dùng máy quét QR hoặc nhập thủ công chuỗi 14 ký tự.

Response `200 OK` là `ApiResponse` với dữ liệu:

```json
{
  "ticketId": 12,
  "bookingId": 45,
  "movieTitle": "Tên phim",
  "cinemaName": "Tên rạp",
  "roomName": "Tên phòng",
  "showtimeStart": "2026-06-15T18:00:00",
  "seatName": "A8",
  "foodFulfillmentStatus": "FULFILLED",
  "foodDeliveredNow": true,
  "foodItems": [
    {
      "foodId": 1,
      "foodName": "Bắp rang bơ",
      "quantity": 2,
      "price": 45000.00
    }
  ]
}
```

Backend kiểm tra QR purpose `TICKET_QR`, đối chiếu ticket, booking và trạng thái suất chiếu. Giờ check-in hợp lệ: `startTime - 30 phút <= now < endTime`. Lần quét vé hợp lệ đầu tiên của booking chuyển trạng thái đồ ăn sang `FULFILLED` và set `foodDeliveredNow: true`; các lần quét vé sau cùng đơn hàng không giao lại đồ ăn. Lỗi được trả theo chuẩn error response của hệ thống.
