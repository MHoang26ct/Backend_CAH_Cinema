**Thông tin chung:**

- **Server:** `http://100.89.144.114:8080`
    
- **Security:** Bearer Auth (JWT Token) - Nhập token vào Header mà không cần tiền tố `Bearer` .
    

## 1. Authentication (Xác thực & Tài khoản)

### Đăng ký tài khoản

- **Endpoint:** `POST /api/v1/auth/register`
    
- **Request Body:**
    
    - `email` (string, **required**)
        
    - `password` (string, **required**)
        
    - `name` (string, **required**)
        
    - `phone` (string)
        
- **Response:** `200 OK` (object)
    

### Đăng nhập (Email/Password)

- **Endpoint:** `POST /api/v1/auth/login`
    
- **Request Body:**
    
    - `email` (string, **required**)
        
    - `password` (string, **required**)
        
- **Response:** `200 OK`

```json
{
  "code": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
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
    }
  }
}
```
    

### Đăng nhập Google

- **Endpoint:** `POST /api/v1/auth/google`
    
- **Request Body:**
    
    - `idToken` (string, **required**)
        

### OTP & Quên mật khẩu

- **Gửi OTP:** `POST /api/v1/auth/send-otp`
    
    - Body: `email` (string, **required**)
        
- **Xác thực OTP:** `POST /api/v1/auth/verify-otp`
    
    - Body: `email` (string, **required**), `otp` (string)
        
- **Xác thực OTP quên mật khẩu:** `POST /api/v1/auth/fp-verify-otp`
    
    - Body: `email` (string, **required**), `otp` (string)
        
- **Đổi mật khẩu sau khi xác thực OTP:** `POST /api/v1/auth/fp-change-password`
    
    - Body: `email` (string, **required**), `newPassword` (string, **required**), `resetToken` (string, **required**)
        

### Quản lý phiên đăng nhập

- **Làm mới Token:** `POST /api/v1/auth/refresh`
    
    - Body: `refreshToken` (string)
        
- **Đăng xuất:** `POST /api/v1/auth/logout`
    
    - Body: `refreshToken` (string)
        
- **Đổi mật khẩu:** `POST /api/v1/auth/change-password`
    
    - Body: `oldPassword` (string, **required**), `newPassword` (string, **required**)
        

## 2. Quản lý Phim (Movies)

### Dành cho Admin

- **Tạo phim mới:** `POST /api/v1/admin/movies/create`
    
- **Cập nhật phim:** `PUT /api/v1/admin/movies/update/{id}`
    
- **Xóa phim:** `DELETE /api/v1/admin/movies/delete/{id}`
    
- **Request Body common (UpdateOrCreateMovieDTO):**
    
    - `title` (string)
        
    - `description` (string)
        
    - `duration` (integer, **min: 15**)
        
    - `releaseDate` (string, format: date)
        
    - `ageRating` (string)
        
    - `posterUrl` (string)
        
    - `trailerUrl` (string)
        
    - `directorName` (string)
        
    - `actorList` (string)
        
    - `genreIdList` (array of int64, **required**)
        
- **Response:** `ApiResponseMovieDetailDTO` chứa thông tin phim và `genres` (mảng GenreDTO).
    

### Công khai (Public)

- **Tìm kiếm/Danh sách phim:** `GET /api/v1/public/movies`
    
    - Query: `title` (string), `genreId` (int64), `ageRating` (string), `pageable` (Pageable object)
    - Mặc định: `size=10, sort="releaseDate,desc"`
        
- **Phim nổi bật (Now showing & Upcoming):** `GET /api/v1/public/movies/featured`
    
    - Trả về 5 phim đang chiếu (releaseDate <= today) và 5 phim sắp chiếu (releaseDate > today).

```json
{
  "code": 200,
  "data": {
    "nowShowing": [
      {
        "movieId": 1,
        "title": "Michael",
        "duration": 130,
        "releaseDate": "2026-04-24",
        "ageRating": "T13",
        "posterUrl": "https://..."
      }
    ],
    "upcoming": [
      {
        "movieId": 3,
        "title": "The Mandalorian and Grogu",
        "duration": 120,
        "releaseDate": "2026-05-22",
        "ageRating": "T13",
        "posterUrl": "https://..."
      }
    ]
  }
}
```

- **Chi tiết phim:** `GET /api/v1/public/movies/{id}`

```json
{
  "code": 200,
  "data": {
    "movieId": 1,
    "title": "Michael",
    "description": "Chân dung điện ảnh về Michael Jackson...",
    "duration": 130,
    "releaseDate": "2026-04-24",
    "ageRating": "T13",
    "posterUrl": "https://...",
    "trailerUrl": "https://www.youtube.com/watch?v=...",
    "directorName": "Antoine Fuqua",
    "actorList": "Jaafar Jackson, Colman Domingo, Nia Long, Miles Teller",
    "genres": [
      { "genreId": 6, "name": "Drama" },
      { "genreId": 10, "name": "Musical" }
    ]
  }
}
```
    
- **Danh sách thể loại:** `GET /api/v1/public/genres/all`
    

## 3. Rạp & Phòng chiếu (Cinemas & Rooms)

### Quản lý Rạp (Admin)

- **Tạo rạp:** `POST /api/v1/admin/cinemas`
    
    - Body: `name` (string, **required**), `address` (string, **required**), `imageUrl` (string), `hotline` (string)
        
- **Cập nhật rạp:** `PUT /api/v1/admin/cinemas/{cinemaId}`
    
    - Body: `cinemaId` (int64, **required**), `name`, `address`, `imageUrl`, `hotline`
        
- **Xóa rạp:** `DELETE /api/v1/admin/cinemas/{cinemaId}`
    
- **Danh sách rạp (Public):** `GET /api/v1/public/cinemas`

```json
{
  "code": 200,
  "message": "Lấy danh sách rạp thành công",
  "data": [
    {
      "cinemaId": 1,
      "name": "CGV Vincom Bà Triệu",
      "address": "191 Bà Triệu, Hai Bà Trưng, Hà Nội",
      "imageUrl": "https://...",
      "hotline": "1900 6017"
    }
  ]
}
```
    

### Quản lý Phòng (Admin)

- **Lấy danh sách phòng theo rạp:** `GET /api/v1/admin/cinemas/{cinemaId}/rooms`
    
- **Tạo phòng:** `POST /api/v1/admin/cinemas/{cinemaId}/rooms`
    
    - Body: `cinemaId` (int64, **required**), `roomName` (string, **required**)
        
- **Cập nhật phòng:** `PUT /api/v1/admin/cinemas/rooms/{roomId}`
    
    - Body: `roomId` (int64, **required**), `roomName` (string, **required**)
        
- **Xóa phòng:** `DELETE /api/v1/admin/cinemas/rooms/{roomId}`
    

## 4. Lịch chiếu (Showtimes)

### Quản lý Lịch chiếu (Admin)

- **Tạo lịch chiếu:** `POST /api/v1/admin/showtime`
    
    - Body: `movieId` (**req**, min 1), `roomId` (**req**, min 1), `format` (enum: 2D, 3D, IMAX), `startTime` (date-time, **req**), `endTime` (date-time, **req**), `basePrice` (number, **req**, > 0)
        
- **Cập nhật lịch chiếu:** `PUT /api/v1/admin/showtime`
    
    - Thêm field: `showtimeId` (int64, **req**, min 1), `status` (enum: AVAILABLE, SOLD_OUT, HIDDEN, **req**)
        
- **Xóa lịch chiếu:** `DELETE /api/v1/admin/showtime/{showtimeId}`
    

### Xem lịch chiếu (Public)

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
    

## 5. Ghế & Đặt vé (Seats & Bookings)

### Quản lý Ghế

- **Tạo sơ đồ ghế (Admin):** `POST /api/v1/admin/seats/create`
    
    - Body: Mảng các object: `roomId` (int64), `row` (number, > 0), `col` (number, > 0), `seatTypeId` (int64)
        
- **Xóa ghế theo phòng:** `DELETE /api/v1/admin/seats/delete/{roomId}`
    
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
    

### Giữ ghế (Locking)

- **Khóa ghế:** `POST /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Mở khóa ghế:** `DELETE /api/v1/seats/{seatId}/unlock` (Query: `showtimeId`)
    
- **Khóa hàng loạt:** `POST /api/v1/seats/pre-lock`
    
    - Body: `showtimeId` (int64), `seatIds` (array of int64)
        

### Đặt vé & Thanh toán

- **Tạo Booking:** `POST /api/v1/bookings`
    
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

- **Xác nhận thanh toán:** `POST /api/v1/bookings/{bookingId}/confirm-payment`
    
    - Body: `paymentRef` (string, **req**), `gateway` (string, **req**)

```json
{
  "code": 200,
  "data": {
    "bookingId": 42,
    "status": "PAID",
    "paymentRef": "VNP20260518001234",
    "gateway": "VNPAY",
    "ticketStatus": "PENDING"
  }
}
```
        

## 6. Voucher & Khuyến mãi

### Dành cho Admin

- **Danh sách voucher:** `GET /api/v1/admin/vouchers` (Query: `pageable`)
    
- **Tạo voucher:** `POST /api/v1/admin/vouchers/create`
    
    - Body: `code`, `type` (FIXED_AMOUNT, PERCENT), `value`, `quantity`, `startAt`, `expiredAt` (**Tất cả required**)
        
- **Cập nhật voucher:** `POST /api/v1/admin/vouchers/update`
    
    - Thêm: `voucherId`, `isActive`, `isDeleted`, `minOrderValue`, `maxDiscount`.
        
- **Xóa voucher:** `DELETE /api/v1/admin/vouchers/{voucherId}`
    

### Dành cho User

- **Lấy voucher của tôi:** `GET /api/v1/user/vouchers`

```json
{
  "code": 200,
  "data": [
    {
      "type": "PERCENT",
      "value": 10.00,
      "maxDiscount": 50000.00,
      "minOrderValue": 200000.00,
      "quantity": 100,
      "usedCount": 23,
      "startAt": "2026-05-01T00:00:00",
      "expiredAt": "2026-05-31T23:59:59"
    }
  ]
}
```
    

## 7. Cấu hình hệ thống (Admin)

### Cấu hình giá (Price Config)

- **Lấy tất cả:** `GET /api/v1/admin/price-config/all`

```json
{
  "code": 200,
  "data": [
    {
      "configId": 1,
      "dayType": "WEEKEND",
      "timeSlot": "EVENING",
      "movieFormat": "3D",
      "multiplier": 1.5
    }
  ]
}
```

- **Cập nhật:** `POST /api/v1/admin/price-config/update`
    
    - Body: `configId` (**req**), `multiplier` (number, **req**), `dayType` (WEEKDAY, WEEKEND, HOLIDAY), `timeSlot` (MORNING, AFTERNOON, EVENING), `movieFormat` (2D, 3D, IMAX)
        

### Quản lý ngày lễ (Holiday)

- **Lấy tất cả:** `GET /api/v1/admin/holiday/all`

```json
{
  "code": 200,
  "data": [
    {
      "holidayId": 1,
      "date": "2026-09-02",
      "name": "Quốc khánh",
      "isRecurring": true
    }
  ]
}
```
    
- **Tạo:** `POST /api/v1/admin/holiday/create`
    
    - Body: `date` (format: date, **req**), `name` (**req**), `isRecurring` (boolean, **req**)
        
- **Cập nhật:** `POST /api/v1/admin/holiday/update` (Thêm `holidayId`)
    
- **Xóa:** `DELETE /api/v1/admin/holiday/delete` (Body: `holidayId`)
    

### Đồ ăn (Food)

#### Dành cho User

- **Danh sách đồ ăn (available):** `GET /api/v1/user/food`
    - Chỉ trả về các món đang available.

```json
{
  "code": 200,
  "message": "Lấy danh sách thức ăn thành công",
  "data": [
    {
      "foodId": 1,
      "name": "Combo Bắp + Nước",
      "description": "Bắp rang bơ lớn + Pepsi lớn",
      "price": 75000.00,
      "category": "COMBO",
      "imageUrl": "https://...",
      "available": true
    }
  ]
}
```

#### Dành cho Admin

- **Danh sách tất cả đồ ăn:** `GET /api/v1/admin/food`
    - Trả về tất cả (bao gồm cả unavailable/deleted).

- **Tạo đồ ăn:** `POST /api/v1/admin/food`
    - Body:
        - `name` (string, **required**)
        - `description` (string)
        - `price` (number, **required**, > 0)
        - `category` (enum, **required**)
        - `imageUrl` (string)
        - `available` (boolean, default: true)

- **Cập nhật đồ ăn:** `PUT /api/v1/admin/food/{id}`
    - Body: giống tạo mới.

- **Xóa đồ ăn (soft delete):** `DELETE /api/v1/admin/food/{id}`

## 8. Báo cáo & Thống kê (Reports - Admin Only)

### Tổng quan kinh doanh
- **Endpoint:** `GET /api/v1/admin/reports/overview`
- **Query Params:**
    - `from` (string, format: date, **required**) - Ngày bắt đầu (yyyy-MM-dd)
    - `to` (string, format: date, **required**) - Ngày kết thúc (yyyy-MM-dd)
- **Mô tả:** Trả về tổng doanh thu, doanh thu vé, doanh thu đồ ăn, số vé bán, AOV... trong khoảng thời gian.
- **Giới hạn:** Tối đa 366 ngày.

```json
{
  "code": 200,
  "data": {
    "from": "2026-05-01",
    "to": "2026-05-18",
    "totalRevenue": 12500000.00,
    "ticketRevenue": 10000000.00,
    "foodRevenue": 2500000.00,
    "totalTicketsSold": 450,
    "totalBookingsPaid": 120,
    "totalDiscount": 500000.00,
    "averageOrderValue": 104166.67
  }
}
```

### Chuỗi doanh thu theo ngày
- **Endpoint:** `GET /api/v1/admin/reports/revenue/daily`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về doanh thu và số lượng bán hàng của từng ngày để vẽ biểu đồ.

```json
{
  "code": 200,
  "data": [
    {
      "date": "2026-05-15",
      "revenue": 3500000.00,
      "bookingCount": 18,
      "ticketCount": 45
    }
  ]
}
```

### Doanh thu theo phim
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-movie`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách phim bán chạy kèm doanh thu, sắp xếp giảm dần.

```json
{
  "code": 200,
  "data": [
    {
      "movieId": 3,
      "movieTitle": "Avengers: Endgame",
      "ticketRevenue": 5600000.00,
      "ticketsSold": 210,
      "bookingCount": 72
    }
  ]
}
```

### Doanh thu theo rạp
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-cinema`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách rạp kèm doanh thu, sắp xếp giảm dần.

```json
{
  "code": 200,
  "data": [
    {
      "cinemaId": 1,
      "cinemaName": "CGV Vincom Center",
      "ticketRevenue": 8200000.00,
      "ticketsSold": 310,
      "bookingCount": 98
    }
  ]
}
```

## 9. Hồ sơ người dùng (User Profile)

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

---

## 10. Bình luận phim (Movie Comments)

### Quy tắc & Điều kiện:
- Chỉ cho phép người dùng đã xem phim (tức là trạng thái booking của phim đó là `CHECKED_IN`) và số lần comment không được lớn hơn số lần xem phim.
- Chỉ người tạo bình luận mới được quyền xóa bình luận của mình.

### Dành cho Public (Không yêu cầu đăng nhập)

- **Lấy danh sách bình luận của phim:** `GET /api/v1/public/comments/movies/{movieId}`
    - Query params:
        - `page` (integer, mặc định `0`)
        - `size` (integer, mặc định `3`)
    - Sắp xếp mặc định: `createdAt,desc` (mới nhất lên đầu)
    - Response: Trả về một `Slice<CommentResponse>` bọc trong `ApiResponse`.
    
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "commentId": 1,
        "userId": 10,
        "userName": "Nguyễn Văn A",
        "userAvatar": "https://...",
        "content": "Phim rất hay và xúc động!",
        "createdAt": "2026-05-22T10:00:00"
      }
    ],
    "pageable": {
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "pageNumber": 0,
      "pageSize": 3,
      "paged": true,
      "unpaged": false
    },
    "size": 3,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "first": true,
    "last": false,
    "numberOfElements": 1,
    "empty": false
  }
}
```

### Dành cho User (Yêu cầu đăng nhập)

- **Thêm bình luận cho phim:** `POST /api/v1/user/comments/movies/{movieId}`
    - Body:
        - `content` (string, **required**, không trống)
    - Response: `200 OK` (bọc trong `ApiResponse`)
    
```json
{
  "code": 200,
  "message": "Thêm bình luận thành công",
  "data": {
    "commentId": 1,
    "userId": 10,
    "userName": "Nguyễn Văn A",
    "userAvatar": "https://...",
    "content": "Phim rất hay và xúc động!",
    "createdAt": "2026-05-22T10:00:00"
  }
}
```

- **Xóa bình luận:** `DELETE /api/v1/user/comments/{commentId}`
    - Response: `200 OK` (bọc trong `ApiResponse`)
    
```json
{
  "code": 200,
  "message": "Xóa bình luận thành công",
  "data": null
}
```

---

## 11. Bài viết khuyến mãi (Promotion Articles)

### Quy tắc hiển thị:
- **Xem danh sách/Preview:** Chỉ trả về thông tin cơ bản: `promotionId`, `title`, `shortDescription`, `imageUrl`, `createdAt`, `isActive`. Tránh tải các trường chi tiết dài như `startDate`, `endDate`, `conditions`, `note` để tối ưu tải trang.
- **Xem chi tiết:** Trả về đầy đủ tất cả các trường thông tin bài viết khuyến mãi.
- Các trường `startDate` và `endDate` (dạng `LocalDate`) chỉ lưu để hiển thị, không mang ý nghĩa logic nghiệp vụ.

### Dành cho Public (Không yêu cầu đăng nhập)

- **Lấy danh sách khuyến mãi (Active):** `GET /api/v1/public/promotions`
    - Phân trang mặc định: `size=9, sort="createdAt,desc"`
    - Chỉ hiển thị các bài viết có `isActive = true`.
    - Response: Trả về một `Page<PromotionArticlePreviewResponse>` bọc trong `ApiResponse`.
    
```json
{
  "code": 200,
  "message": "Lấy danh sách khuyến mãi thành công",
  "data": {
    "content": [
      {
        "promotionId": 1,
        "title": "Khuyến mãi hè cực khủng",
        "shortDescription": "Nhận ngay combo nước bắp miễn phí khi mua 2 vé xem phim...",
        "imageUrl": "https://...",
        "createdAt": "2026-05-22T08:00:00",
        "isActive": true
      }
    ],
    "pageable": {
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "pageNumber": 0,
      "pageSize": 9,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 1,
    "size": 9,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "first": true,
    "last": true,
    "numberOfElements": 1,
    "empty": false
  }
}
```

- **Chi tiết khuyến mãi:** `GET /api/v1/public/promotions/{id}`
    - Yêu cầu bài viết đó có `isActive = true`.
    - Response: Trả về `PromotionArticleResponse` bọc trong `ApiResponse`.
    
```json
{
  "code": 200,
  "message": "Lấy chi tiết khuyến mãi thành công",
  "data": {
    "promotionId": 1,
    "title": "Khuyến mãi hè cực khủng",
    "shortDescription": "Nhận ngay combo nước bắp miễn phí khi mua 2 vé xem phim...",
    "startDate": "2026-06-01",
    "endDate": "2026-08-31",
    "conditions": "Áp dụng cho mọi khách hàng đặt vé qua app/website từ ngày 1/6/2026.",
    "imageUrl": "https://...",
    "note": "Mỗi tài khoản chỉ được áp dụng 1 lần.",
    "createdAt": "2026-05-22T08:00:00",
    "updatedAt": "2026-05-22T08:00:00",
    "isActive": true
  }
}
```

### Dành cho Admin

- **Lấy danh sách tất cả khuyến mãi:** `GET /api/v1/admin/promotions`
    - Query params: `page` (mặc định `0`), `size` (mặc định `10`)
    - Trả về cả bài viết Active lẫn Inactive.
    - Response: Trả về trực tiếp `Page<PromotionArticlePreviewResponse>` (không bọc trong `ApiResponse`).
    
```json
{
  "content": [
    {
      "promotionId": 1,
      "title": "Khuyến mãi hè cực khủng",
      "shortDescription": "Nhận ngay combo nước bắp miễn phí...",
      "imageUrl": "https://...",
      "createdAt": "2026-05-22T08:00:00",
      "isActive": true
    }
  ],
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

- **Chi tiết khuyến mãi (Admin):** `GET /api/v1/admin/promotions/{id}`
    - Response: Trả về trực tiếp `PromotionArticleResponse`.
    
- **Tạo khuyến mãi mới:** `POST /api/v1/admin/promotions`
    - Request Body (PromotionArticleRequest):
        - `title` (string, **required**, không trống)
        - `shortDescription` (string, **required**, không trống)
        - `startDate` (string, format: `yyyy-MM-dd`, có thể null)
        - `endDate` (string, format: `yyyy-MM-dd`, có thể null)
        - `conditions` (string, có thể null)
        - `imageUrl` (string, có thể null)
        - `note` (string, có thể null)
        - `isActive` (boolean, mặc định `true`)
    - Response: `201 Created` chứa `PromotionArticleResponse` vừa tạo.
    
- **Cập nhật khuyến mãi:** `PUT /api/v1/admin/promotions/{id}`
    - Body: giống tạo mới.
    - Response: `200 OK` chứa `PromotionArticleResponse` sau khi cập nhật.
    
- **Xóa khuyến mãi:** `DELETE /api/v1/admin/promotions/{id}`
    - Response: `204 No Content`