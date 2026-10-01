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
| `POST` | `/api/v1/staff/bookings/{bookingId}/confirm-payment` | `paymentRef`, `gateway` bắt buộc |

Endpoint này yêu cầu `ROLE_STAFF` hoặc `ROLE_ADMIN`.

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
| `POST` | `/api/v1/bookings/{bookingId}/momo/pay` | `requestId` bắt buộc; `requestType` tùy chọn |
| `POST` | `/api/v1/bookings/{bookingId}/vnpay/pay` | `requestId` bắt buộc |

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
