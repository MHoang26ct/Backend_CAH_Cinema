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
    
    - **Auth:** Yêu cầu quyền `ROLE_STAFF` hoặc `ROLE_ADMIN`.
    - **Giới hạn hiện tại:** service còn yêu cầu người xác nhận là chủ booking; STAFF/ADMIN xác nhận booking của khách khác nhận 403 `FORBIDDEN`. Chưa hỗ trợ đầy đủ nghiệp vụ thu ngân cho khách khác.
    - Booking phải PENDING và còn hạn. Cùng `paymentRef` cho cùng booking trả kết quả đã ghi nhận; dùng lại cho booking khác bị từ chối.
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

## Tra cứu công khai và thao tác của người đã đăng nhập

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

- **Khóa ghế (Yêu cầu đăng nhập):** `POST /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Mở khóa ghế (Yêu cầu đăng nhập):** `DELETE /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Khóa hàng loạt (Yêu cầu đăng nhập):** `POST /api/v1/seats/pre-lock`
    
    - Body: `showtimeId` (int64), `seatIds` (array of int64)

**Quy tắc giữ ghế:** giữ tạm 5 phút theo người dùng và suất chiếu; chỉ chủ khóa được mở khóa. Tạo booking yêu cầu tất cả khóa còn thuộc người tạo và nâng thời hạn khóa lên 15 phút. Ghế phải đúng phòng, chưa bán, không phải lối đi; danh sách không trùng/rỗng. Ghế đôi cần chọn cùng ghế đôi kề bên trong cùng hàng, dùng `pre-lock` để gửi nhiều ghế.

**Cửa sổ đặt vé:** suất phải AVAILABLE, ngày chiếu trong tối đa 7 ngày tới và `now + 15 phút <= startTime + thời lượng đã chốt / 2`. Quy tắc áp dụng cả giữ ghế lẫn tạo booking. Không đủ điều kiện trả `SHOWTIME_BOOKING_CLOSED` (409), hoặc `VALIDATION_FAILED` (400) nếu quá giới hạn ngày.

### Đặt vé (Booking)

- **Tạo Booking (Yêu cầu đăng nhập):** `POST /api/v1/bookings`
    
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
    "lateDiscountAmount": 0.00,
    "voucherDiscountAmount": 26950.00,
    "totalAmount": 242550.00
  }
}
```

**Các khoản tiền trong response:**

- `seatSubtotal`, `foodSubtotal`: tiền ghế/đồ ăn sau giảm theo hạng thành viên, trước giảm muộn/voucher. Giá trên từng vé lưu trước giảm hạng/voucher/giảm muộn, không nhất thiết cộng lại bằng `seatSubtotal`.
- `lateDiscountAmount`: giảm 50% tiền ghế sau giảm hạng khi tạo đơn sau hơn 15 phút đầu phim; làm tròn 2 chữ số HALF_UP, không giảm muộn đồ ăn. Đúng phút thứ 15 chưa được giảm.
- `voucherDiscountAmount`: giảm bằng voucher; gửi voucher cho đơn giảm muộn bị từ chối với `DISCOUNT_NOT_COMBINABLE` (400).
- `discountAmount = lateDiscountAmount + voucherDiscountAmount`; `totalAmount = max(0, seatSubtotal + foodSubtotal - discountAmount)`.
- Giá chốt lúc tạo booking. `expiresAt` là thời điểm tạo + 15 phút; tại đúng mốc đó đã hết hạn thanh toán. Tác vụ nền cập nhật EXPIRED sau đó, nên trạng thái lưu có thể còn PENDING trong thời gian ngắn dù đã không được thanh toán.

- **Lấy trạng thái Booking (User):** `GET /api/v1/bookings/{bookingId}`
    - Chỉ chủ booking được đọc; người khác nhận 403. Dùng để polling kết quả thanh toán.
    - CHECKED_IN nghĩa là ít nhất một vé đã dùng; REFUNDED là trạng thái nội bộ, chưa chứng minh đã hoàn tiền qua cổng. Enum còn có CANCELLED nhưng chưa có luồng chuyển booking sang trạng thái này.

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

Cả hai API khởi tạo yêu cầu người gọi sở hữu booking, phương thức booking khớp cổng, trạng thái PENDING và chưa hết hạn. Nếu đã có yêu cầu CREATED còn hiệu lực, server có thể trả lại URL/QR cũ dù `requestId` khác; không mặc định mỗi lần gọi tạo giao dịch mới. Trình duyệt quay về trang thành công chưa đủ để xác nhận PAID; frontend cần đọc trạng thái booking.

VNPay mặc định dùng sandbox; endpoint MoMo phụ thuộc cấu hình môi trường. Hiện số tiền gửi cổng được chuyển sang số nguyên; cần đối soát nếu giá chốt có phần lẻ. IPN đến sau hạn có thể bị từ chối dù cổng đã thu tiền, chưa có quy trình hoàn tự động.


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

### Lỗi nghiệp vụ thường gặp

| HTTP | Mã | Tình huống |
|---|---|---|
| 400 | SEAT_ALREADY_BOOKED | Ghế đã bán/đang bị giữ hoặc khóa không còn thuộc người tạo |
| 400 | VALIDATION_FAILED | Sai phòng, danh sách ghế sai, voucher không hợp lệ hoặc ngoài giới hạn ngày |
| 400 | DISCOUNT_NOT_COMBINABLE | Kết hợp voucher với giảm giá vào xem muộn |
| 403 | FORBIDDEN | Sai chủ booking/khóa ghế hoặc thiếu quyền thao tác |
| 409 | SHOWTIME_BOOKING_CLOSED | Suất ngừng nhận đặt vé |
| 409 | BOOKING_EXPIRED | Đã đến hạn thanh toán |
| 409 | BOOKING_INVALID_STATUS | Trạng thái/phương thức booking không phù hợp |
| 409 | PAYMENT_REF_DUPLICATE | Mã tham chiếu đã dùng cho booking khác |
| 409 | PAYMENT_ALREADY_CONFIRMED | Đơn đã thanh toán, nhận xác nhận mới không phải mã đã ghi nhận |

IPN dùng phản hồi riêng theo từng cổng: MoMo trả 204 khi xử lý bình thường; VNPay trả object có `RspCode`/`Message`, cần đọc mã trong body ngay cả khi HTTP 200. VNPay kiểm tra chữ ký và số tiền; MoMo có kiểm tra chữ ký nhưng chưa thấy đối chiếu số tiền IPN với yêu cầu đã lưu. Xem thêm giới hạn thanh toán/hủy suất tại [SRS mục 7](../../SRS.md#7-nghiệm-thu-và-giới-hạn).
