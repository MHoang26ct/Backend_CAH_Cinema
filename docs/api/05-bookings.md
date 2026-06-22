# 5. Ghế & Đặt vé (Seats & Bookings)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

### Quản lý Ghế

- **Tạo sơ đồ ghế:** `POST /api/v1/admin/seats/create`
    
    - Body: Mảng các object: `roomId` (int64), `row` (number, > 0), `col` (number, > 0), `seatTypeId` (int64)

- **Lấy sơ đồ ghế gốc theo phòng:** `GET /api/v1/admin/seats/rooms/{roomId}`
    - Dùng để xem cấu hình sơ đồ ghế hiện tại của một phòng chiếu.

- **Xóa ghế theo phòng:** `DELETE /api/v1/admin/seats/delete/{roomId}`
    
- **Thay thế sơ đồ ghế (Room Cloning):** `PUT /api/v1/admin/seats/replace`
    - Body: `roomId` (int64, **req**), `seats` (Mảng các object: `roomId` (int64), `row` (number, > 0), `col` (number, > 0), `seatTypeId` (int64))
    - Chức năng: Thay thế sơ đồ ghế bằng cách tạo phòng mới. Showtime > 7 ngày tới sẽ tự động migrate sang phòng mới. Showtime ≤ 7 ngày giữ nguyên phòng cũ đến khi chiếu xong.

## Dành cho Staff / Admin

- **Xác nhận thanh toán thủ công:** `POST /api/v1/staff/bookings/{bookingId}/confirm-payment`
    
    - **Auth:** Yêu cầu quyền `ROLE_STAFF` hoặc `ROLE_ADMIN`
    - **Body:** `paymentRef` (string, **req**), `gateway` (string, **req**)

```json
{
  "code": 200,
  "data": {
    "bookingId": 42,
    "status": "PAID",
    "paymentRef": "CASH20260518001234",
    "gateway": "CASH",
    "ticketStatus": "PENDING"
  }
}
```

## Dành cho Public / User

### Xem và Giữ ghế

- **Lấy ghế theo lịch chiếu (Public):** `GET /api/v1/public/seats` (Query: `showtimeId`, **required**)

```json
{
  "code": 200,
  "data": [
    {
      "seatId": 101,
      "row": 3.0,
      "col": 5.0,
      "rowLabel": "C",
      "colLabel": "5",
      "seatType": { "seatTypeId": 1, "name": "VIP", "priceMultiplier": 1.5 },
      "status": "ACTIVE",
      "isLocked": false,
      "isSold": false,
      "occupancyStatus": "AVAILABLE"
    }
  ]
}
```

- **Khóa ghế (Public):** `POST /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Mở khóa ghế (Public):** `DELETE /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Khóa hàng loạt (Public):** `POST /api/v1/seats/pre-lock`
    
    - Body: `showtimeId` (int64), `seatIds` (array of int64)

### Đặt vé (Booking)

- **Tạo Booking (Public/User):** `POST /api/v1/bookings`
    
    - Body: `showtimeId` (**req**), `seatIds` (array, **req**), `paymentMethod` (enum: CASH, VNPAY, MOMO, **req**), `voucherId` (int64), `foodItems` (mảng object: `foodId`, `quantity` min 1)

```json
{
  "code": 200,
  "data": {
    "bookingId": 42,
    "status": "PENDING",
    "expiresAt": "2026-05-18T18:15:00",
    "seatSubtotal": 196000.00,
    "foodSubtotal": 73500.00,
    "discountAmount": 26950.00,
    "totalAmount": 242550.00
  }
}
```

- **Lấy trạng thái Booking (User):** `GET /api/v1/bookings/{bookingId}`
    - Dùng để Frontend polling kết quả thanh toán sau khi quét QR MoMo/VNPay.

```json
{
  "code": 200,
  "data": {
    "bookingId": 42,
    "status": "PAID"
  }
}
```

### Thanh toán điện tử (MoMo & VNPay)

- **Tạo đơn thanh toán MoMo (Yêu cầu đăng nhập):** `POST /api/v1/bookings/{bookingId}/momo/pay`
    - **Auth:** Yêu cầu đăng nhập (`authenticated()`).
    - **Request Body:**
      - `requestId` (string, **required**) - UUID làm Idempotency key.
      - `requestType` (string) - `captureWallet` (Ví MoMo), `payWithATM` (Thẻ ATM nội địa), `payWithCC` (Thẻ quốc tế).
    - **Response:** `200 OK`
    ```json
    {
      "code": 200,
      "message": "Tạo đơn thanh toán MoMo thành công",
      "data": {
        "payUrl": "https://payment.momo.vn/...",
        "deeplink": "momo://...",
        "qrCodeUrl": "https://...",
        "momoOrderId": "42_1716382910000"
      }
    }
    ```

- **Tạo đơn thanh toán VNPay (Yêu cầu đăng nhập):** `POST /api/v1/bookings/{bookingId}/vnpay/pay`
    - **Auth:** Yêu cầu đăng nhập (`authenticated()`).
    - **Request Body:**
      - `requestId` (string, **required**) - UUID làm Idempotency key.
    - **Response:** `200 OK`
    ```json
    {
      "code": 200,
      "message": "Tạo đơn thanh toán VNPay thành công",
      "data": {
        "payUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
        "vnpayOrderId": "42_1716382910000"
      }
    }
    ```

- **Callback IPN MoMo (Public):** `POST /api/v1/public/momo/ipn`
    - **Mô tả:** Server MoMo gọi tự động server-to-server sau khi giao dịch hoàn tất.
    - **Response:** `204 No Content`

- **Callback IPN VNPay (Public):** `GET /api/v1/public/vnpay/ipn`
    - **Mô tả:** Server VNPay gọi tự động server-to-server sau khi giao dịch hoàn tất.
    - **Response:** `200 OK` (VnpayIpnResponse).
