# 12. Check-in vé

## Staff / Admin

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/staff/tickets/check-in` | `{ "qrToken": "..." }` |

Endpoint yêu cầu `ROLE_STAFF` hoặc `ROLE_ADMIN`. `qrToken` không được để trống.

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
