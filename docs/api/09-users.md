# 9. Hồ sơ người dùng (User Profile)

## Dành cho User (Yêu cầu đăng nhập)

> **Auth required:** Tất cả endpoint trong mục này yêu cầu JWT hợp lệ (Bearer Token).

### Xem hồ sơ cá nhân

- **Endpoint:** `GET /api/v1/users/me`
- **Mô tả:** Trả về thông tin cơ bản của user đang đăng nhập kèm **5 booking gần nhất** (chỉ tính status `PAID` / `CHECKED_IN`) với đầy đủ chi tiết để hiển thị như hóa đơn.
- **Response:** `200 OK`

```json
{
  "code": 200,
  "message": "Lấy thông tin profile thành công",
  "data": {
    "user": {
      "userId": 1,
      "name": "Nguyễn Văn A",
      "email": "a@example.com",
      "phone": "0901234567",
      "avatarUrl": "https://...",
      "authProvider": "EMAIL",
      "role": "ROLE_USER",
      "totalPaid": 1200000.00,
      "totalPoint": 30,
      "rankLevel": "SILVER"
    },
    "recentInvoices": [
      {
        "bookingId": 42,
        "bookingStatus": "PAID",
        "paymentMethod": "VNPAY",
        "discountAmount": 50000.00,
        "totalPrice": 350000.00,
        "bookingCreatedAt": "2026-05-10T15:30:00",
        "voucherCode": "SUMMER10",
        "showtimeId": 7,
        "movieFormat": "2D",
        "startTime": "2026-05-10T18:00:00",
        "endTime": "2026-05-10T20:10:00",
        "movieId": 3,
        "movieTitle": "Avengers: Endgame",
        "moviePosterUrl": "https://...",
        "cinemaName": "CGV Vincom Bà Triệu",
        "roomName": "Hall 1",
        "seats": [
          {
            "seatId": 101,
            "seatRow": 3.0,
            "seatCol": 5.0,
            "seatType": "VIP",
            "ticketPrice": 100000.00
          }
        ],
        "foods": [
          {
            "foodId": 2,
            "foodName": "Combo Bắp + Nước",
            "foodImageUrl": "https://...",
            "foodCategory": "Combo",
            "quantity": 1,
            "unitPrice": 75000.00
          }
        ],
        "foodTotalPrice": 75000.00
      }
    ]
  }
}
```

**Hệ thống hạng thành viên (Loyalty):**

| Hạng | Điểm tích lũy | Giảm giá |
|---|---|---|
| SILVER | 0 – 500 điểm | 2% |
| GOLD | 501 – 1500 điểm | 3% |
| DIAMOND | > 1500 điểm | 5% |

> 1 điểm = 40,000 VNĐ trong `totalPaid`. Điểm và hạng được cập nhật tự động sau khi booking được xác nhận thanh toán. Giảm giá theo hạng được áp dụng tự động vào subtotal (ghế + đồ ăn) trước khi áp voucher.

---

### Cập nhật hồ sơ cá nhân

- **Endpoint:** `PATCH /api/v1/users/me`
- **Mô tả:** Cập nhật một phần thông tin cá nhân. Chỉ field nào được gửi lên mới thay đổi (partial update).
- **Request Body:**

| Field | Type | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `name` | string | Không | 2 – 100 ký tự |
| `email` | string | Không | Định dạng email hợp lệ |
| `phone` | string | Không | 9 – 11 ký tự |
| `avatarUrl` | string | Không | URL ảnh đại diện |

- **Response:** `200 OK` — trả về `UserDTO` đã cập nhật.

```json
{
  "code": 200,
  "message": "Cập nhật profile thành công",
  "data": {
    "userId": 1,
    "name": "Nguyễn Văn B",
    "email": "b@example.com",
    "phone": "0909876543",
    "avatarUrl": "https://...",
    "authProvider": "EMAIL",
    "role": "ROLE_USER",
    "totalPaid": 1200000.00,
    "totalPoint": 30,
    "rankLevel": "SILVER"
  }
}
```
