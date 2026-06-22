# 4. Lịch chiếu (Showtimes)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

- **Tạo lịch chiếu:** `POST /api/v1/admin/showtime`
    
    - Body: `movieId` (**req**, min 1), `roomId` (**req**, min 1), `format` (enum: 2D, 3D, IMAX), `startTime` (date-time, **req**), `endTime` (date-time, **req**), `basePrice` (number, **req**, > 0)
        
- **Cập nhật lịch chiếu:** `PUT /api/v1/admin/showtime`
    
    - Thêm field: `showtimeId` (int64, **req**, min 1), `status` (enum: AVAILABLE, SOLD_OUT, HIDDEN, **req**)
        
- **Xóa lịch chiếu:** `DELETE /api/v1/admin/showtime/{showtimeId}`
    
- **Xem lịch chiếu theo phòng:** `GET /api/v1/admin/showtime/rooms/{roomId}` (Query: `date` format: date, **required**)
    - Xem toàn bộ showtime của phòng theo ngày (bao gồm mọi status: AVAILABLE, SOLD_OUT, HIDDEN, CANCELLED).
        
- **Hủy lịch chiếu hàng loạt:** `POST /api/v1/admin/showtime/cancel-by-room`
    - Body: `roomId` (int64, **req**), `fromDate` (format: date, **req**), `toDate` (format: date, **req**), `reason` (string)
    - Tự động refund booking PAID và hủy booking PENDING, tự động gửi email thông báo cho khách hàng.

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

```json
{
  "code": 200,
  "data": {
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
        "basePrice": 75000.00,
        "status": "AVAILABLE",
        "roomName": "Hall 1"
      }
    ]
  }
}
```
