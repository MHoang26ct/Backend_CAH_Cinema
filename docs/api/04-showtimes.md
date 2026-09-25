# 4. Lịch chiếu (Showtimes)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

- **Tạo lịch chiếu:** `POST /api/v1/admin/showtime`
    
    - Body: `movieId` (**req**, min 1), `roomId` (**req**, min 1), `format` (enum: 2D, 3D, IMAX, **req**), `startTime` (date-time, **req**), `endTime` (date-time, tùy chọn; chấp nhận để tương thích nhưng không dùng tính lịch), `basePrice` (number, **req**, > 0)
        
- **Cập nhật lịch chiếu:** `PUT /api/v1/admin/showtime`
    
    - Thêm field: `showtimeId` (int64, **req**, min 1), `status` (enum: AVAILABLE, SOLD_OUT, HIDDEN, CANCELLED, **req**)
        
- **Xóa lịch chiếu:** `DELETE /api/v1/admin/showtime/{showtimeId}`
    - Xóa mềm; không chạy quy trình hủy booking/hoàn tiền. Không dùng thay cho `cancel-by-room`.
    
- **Xem lịch chiếu theo phòng:** `GET /api/v1/admin/showtime/rooms/{roomId}` (Query: `date` format: date, **required**)
    - Xem toàn bộ showtime của phòng theo ngày (bao gồm mọi status: AVAILABLE, SOLD_OUT, HIDDEN, CANCELLED).
        
- **Hủy lịch chiếu hàng loạt:** `POST /api/v1/admin/showtime/cancel-by-room`
    - Body: `roomId` (int64, **req**), `fromDate` (format: date, **req**), `toDate` (format: date, **req**), `reason` (string)
    - Chọn các suất AVAILABLE của phòng trong khoảng ngày, tính cả ngày cuối; `fromDate <= toDate`.
    - Chuyển suất sang CANCELLED. Booking PAID được ghi REFUNDED nội bộ, trả lượt voucher và tạo tác vụ email; chưa có gọi API hoàn tiền qua cổng.
    - **Giới hạn hiện tại:** PENDING chỉ chuyển EXPIRED nếu đã đến hạn; đơn còn hạn chưa bị kết thúc ngay. CHECKED_IN không nằm trong nhóm booking được xử lý. Các bước hủy suất/xử lý đơn có thể hoàn tất một phần khi lỗi; cần đối soát, không coi response thành công là bằng chứng tiền đã hoàn.
    - Không dùng PUT đổi `status` sang CANCELLED để thay quy trình này: PUT không gọi xử lý booking liên quan.

### Quy tắc thời gian và lỗi

- Tạo suất trong tương lai, tối đa 30 ngày tới. Giờ bắt đầu từ 08:00 đến hết ngày; giờ kết thúc không muộn hơn 02:00 hôm sau. Khoảng nghỉ giữa hai suất cùng phòng tối thiểu 30 phút.
- Server tính `endTime = startTime + movie.duration` và chốt thời lượng khi tạo. Chỉ đổi giờ thì giữ thời lượng đã chốt; đổi phim thì lấy thời lượng phim mới. Sửa thời lượng danh mục phim không tự cập nhật suất cũ.
- PUT cần đầy đủ dữ liệu như tạo, thêm `showtimeId` và `status`; `format` cũng cần giá trị hợp lệ để vượt kiểm tra service. Không phải PATCH cập nhật từng trường.
- Đổi giờ/phòng/phim chỉ trước lúc suất bắt đầu và khi không có booking PENDING còn hạn, PAID hoặc CHECKED_IN. Vi phạm trả HTTP 409, `SHOWTIME_SCHEDULE_LOCKED`.
- Dữ liệu thời gian sai/trùng lịch trả HTTP 400, `VALIDATION_FAILED`. Kiểm tra lịch qua nửa đêm còn có giới hạn được ghi trong SRS.

## Dành cho Public (Không yêu cầu đăng nhập)

> **Lưu ý:** Chỉ có thể xem lịch chiếu trong vòng tối đa 7 ngày tới kể từ ngày hiện tại.

- **Theo phim:** `GET /api/v1/public/showtimes/movies/{movieId}` (Query: `date` format: date, **required**)

```json
{
  "code": 200,
  "data": {
    "movie": {
      "movieId": 3,
      "title": "Avengers: Endgame",
      "description": "..."
    },
    "cinemas": [
      {
        "cinemaId": 1,
        "cinemaName": "CGV Vincom Bà Triệu",
        "address": "191 Bà Triệu, HN",
        "showtimes": [
          {
            "showtimeId": 7,
            "startTime": "2026-05-18T18:00:00",
            "endTime": "2026-05-18T20:10:00",
            "format": "2D",
            "basePrice": 75000.00,
            "status": "AVAILABLE",
            "roomName": "Hall 1"
          }
        ]
      }
    ]
  }
}
```

- **Theo rạp:** `GET /api/v1/public/showtimes/cinemas/{cinemaId}` (Query: `date` format: date, **required**)

**Response:** `data` là danh sách nhóm lịch theo phim, không phải một object đơn.

```json
{
  "code": 200,
  "data": [
    {
      "movie": {
        "movieId": 3,
        "title": "Avengers: Endgame",
        "posterUrl": "https://...",
        "ageRating": "T13"
      },
      "showtimes": [
        {
          "showtimeId": 7,
          "startTime": "2026-05-18T18:00:00",
          "endTime": "2026-05-18T20:10:00",
          "format": "2D",
          "basePrice": 75000.0,
          "status": "AVAILABLE",
          "roomName": "Hall 1"
        }
      ]
    }
  ]
}
```
