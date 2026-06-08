# 12. Nghiệp vụ Check-in vé (Tickets)

## Dành cho Staff / Admin

- **Check-in vé bằng QR Code:** `POST /api/v1/staff/tickets/check-in`
    - **Auth:** Yêu cầu đăng nhập với role `ROLE_STAFF` hoặc `ROLE_ADMIN`.
    - **Request Body:**
      - `qrToken` (string, **required**) - Chuỗi JWT mã hóa thông tin vé chứa trong QR Code.
    - **Response:** `200 OK`
    ```json
    {
      "code": 200,
      "message": "Check-in vé thành công",
      "data": {
        "ticketId": 12,
        "movieTitle": "Michael",
        "cinemaName": "CGV Vincom Bà Triệu",
        "roomName": "Hall 1",
        "startTime": "2026-06-15T18:00:00",
        "seatName": "VIP H-08",
        "checkedInAt": "2026-06-15T17:45:12"
      }
    }
    ```
