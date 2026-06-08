# Hướng Dẫn Tích Hợp Xử Lý Kết Quả Thanh Toán Cho Frontend

Tài liệu này hướng dẫn đội ngũ Frontend (FE) cách nhận và xử lý kết quả thanh toán sau khi người dùng thực hiện thanh toán qua các cổng MoMo và VNPay, cũng như cách kết hợp với hệ thống Backend.

---

## 1. Các Phương Thức Nhận Kết Quả Thanh Toán

Khi người dùng thanh toán qua MoMo hoặc VNPay, FE có thể nhận biết giao dịch thành công hay thất bại qua **2 cách chính**:

### Cách 1: Thông qua `ReturnUrl` (Redirect từ Cổng Thanh Toán)
Dành cho trường hợp thanh toán trên cùng một thiết bị (ví dụ: mở app MoMo/VNPay từ Web Mobile, hoặc thanh toán ATM/Visa trực tiếp trên trình duyệt).

- **Quy trình:** Sau khi thanh toán xong, cổng thanh toán sẽ tự động chuyển hướng (redirect) người dùng về trang Web của chúng ta (URL này do BE truyền lúc tạo đơn, ví dụ: `https://domain.com/payment/result`).
- **Dữ liệu:** Các thông số kết quả sẽ được đính kèm trên thanh địa chỉ dưới dạng URL Query Parameters (GET).
  - **MoMo:** `?resultCode=0&message=Success&orderId=...`
  - **VNPay:** `?vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_TxnRef=...`
- **Nhiệm vụ của FE:**
  1. Đọc các Query Parameters trên URL.
  2. Dựa vào mã lỗi (`resultCode` của MoMo hoặc `vnp_ResponseCode` của VNPay) để hiển thị giao diện Thành công hoặc Thất bại ngay lập tức. *(Mã `0` đối với MoMo và `00` đối với VNPay là Thành công).*
  3. **Lưu ý quan trọng:** Không gọi API cập nhật trạng thái đơn hàng từ FE. Việc này do BE tự xử lý ngầm qua `IPN (Webhook)`.

### Cách 2: Thông qua API Polling (Chờ kết quả)
Dành cho trường hợp **Thanh toán bằng mã QR tĩnh/động**. (Ví dụ: Khách hàng mua vé trên màn hình máy tính, nhưng dùng điện thoại quét mã QR để thanh toán).

- **Vấn đề:** Trình duyệt trên máy tính sẽ **không** nhận được lệnh chuyển hướng (redirect) vì giao dịch xảy ra trên điện thoại.
- **Giải pháp:** FE phải chủ động hỏi BE xem đơn hàng này đã được thanh toán xong chưa bằng cơ chế **Polling**.

---

## 2. Hướng Dẫn Tích Hợp API Polling Cho Mã QR

Backend đã cung cấp một API chuyên dụng để FE kiểm tra trạng thái của Booking.

### API Lấy Trạng Thái Booking

`GET /api/v1/bookings/{bookingId}`

**Headers:**
- `Authorization: Bearer {accessToken}`

**Response Trả Về (JSON):**
```json
{
  "code": 200,
  "message": "Lấy trạng thái booking thành công",
  "data": {
    "bookingId": 42,
    "status": "PAID" // Hoặc PENDING, FAILED, CANCELLED, REFUNDED
  }
}
```

### Luồng Xử Lý (Flow) Trên Frontend:

1. Khi người dùng chọn thanh toán QR và FE hiển thị mã QR lên màn hình.
2. FE bắt đầu gọi hàm `setInterval` để gọi API `GET /api/v1/bookings/{bookingId}` lặp lại mỗi **3 đến 5 giây**.
3. **Kiểm tra trường `status` trong response:**
   - Nếu `status === 'PENDING'`: Tiếp tục chờ (tiếp tục polling).
   - Nếu `status === 'PAID'`: 
     - Dừng polling (`clearInterval`).
     - Tự động chuyển hướng người dùng sang trang "Thanh Toán Thành Công" và hiển thị thông tin vé.
   - Nếu `status === 'FAILED'` hoặc hết thời gian chờ (15 phút):
     - Dừng polling.
     - Hiển thị thông báo "Thanh toán thất bại hoặc hết hạn".

**Mã giả (Pseudo-code) tham khảo:**

```javascript
let pollInterval;

function startPolling(bookingId) {
  pollInterval = setInterval(async () => {
    try {
      const response = await fetch(`/api/v1/bookings/${bookingId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const resData = await response.json();
      
      if (resData.data.status === 'PAID') {
        clearInterval(pollInterval);
        redirectToSuccessPage(bookingId);
      } else if (resData.data.status === 'FAILED') {
        clearInterval(pollInterval);
        showError("Thanh toán thất bại");
      }
      // PENDING -> Do nothing, continue next tick
    } catch (error) {
      console.error("Lỗi khi polling", error);
    }
  }, 3000); // 3 giây 1 lần
}

// Khi rời khỏi trang hoặc timeout (ví dụ sau 15 phút)
function stopPolling() {
  if (pollInterval) clearInterval(pollInterval);
}
```

---

## 3. Tổng Kết Các Quy Tắc Dành Cho Frontend

1. **Hiển thị thông tin tức thời qua URL (ReturnUrl):** Rất tốt để cho trải nghiệm người dùng nhanh chóng trên Mobile Web / App.
2. **Luôn dự phòng Polling:** Khi người dùng đang ở trang chờ thanh toán (có QR), bắt buộc phải dùng Polling API `GET /api/v1/bookings/{bookingId}`.
3. **Nguồn Sự Thật (Single Source of Truth):** Trạng thái từ API của Backend (thông qua Polling hoặc IPN ngầm) luôn là trạng thái chính xác nhất. Đừng bao giờ chỉ tin tưởng vào URL Redirect vì người dùng có thể tự gõ đổi URL hoặc đóng trình duyệt sớm.
4. **Bảo mật:** Không bao giờ gọi API cập nhật Database trực tiếp từ FE bằng các dữ liệu nhận được từ URL của Cổng thanh toán.
