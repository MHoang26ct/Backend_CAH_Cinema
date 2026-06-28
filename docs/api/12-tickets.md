# 12. Nghiệp vụ Check-in vé (Tickets)

## Dành cho Staff / Admin

- **Check-in vé bằng QR Code:** `POST /api/v1/staff/tickets/check-in`
    - **Auth:** Yêu cầu đăng nhập với role `ROLE_STAFF` hoặc `ROLE_ADMIN`.
    - **Request Body:**
      - `qrToken` (string, **required**) - Chuỗi JWT mã hóa thông tin vé chứa trong QR Code.
    - **Hành vi:**
      - Cập nhật trạng thái vé (`isCheckedIn = true`).
      - Nếu booking đang ở trạng thái `PAID`, tự động chuyển sang `CHECKED_IN` ngay khi vé đầu tiên trong booking được check-in.
    - **Response:** `200 OK`
    ```json
    {
      "code": 200,
      "message": "Check-in vé thành công",
      "data": {
        "ticketId": 12,
        "bookingId": 45,
        "movieTitle": "Michael",
        "cinemaName": "CGV Vincom Bà Triệu",
        "roomName": "Hall 1",
        "showtimeStart": "2026-06-15T18:00:00",
        "seatName": "A8"
      }
    }
    ```
    - **Lỗi có thể xảy ra:**

      | HTTP Status | Error Code | Mô tả |
      |---|---|---|
      | `400` | `TICKET_INVALID_QR` | Mã QR không hợp lệ, hết hạn, thiếu thông tin, hoặc không khớp dữ liệu trong hệ thống |
      | `400` | `VALIDATION_FAILED` | Chưa đến ngày chiếu (chỉ được check-in trước tối đa 24h) hoặc suất chiếu đã kết thúc quá 4 tiếng |
      | `404` | `RESOURCE_NOT_FOUND` | Không tìm thấy vé hoặc booking trong hệ thống |
      | `409` | `TICKET_ALREADY_USED` | Vé đã được check-in trước đó |
