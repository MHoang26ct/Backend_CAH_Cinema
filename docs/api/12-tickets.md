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
  "seatName": "A8"
}
```

Backend kiểm tra QR/ticket và điều kiện check-in trước khi cập nhật trạng thái vé. Lỗi được trả theo chuẩn error response của hệ thống.
