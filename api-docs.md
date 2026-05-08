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
        
- **Response:** `200 OK` (object)
    

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
    
    - Query: `title` (string), `genreId` (int64), `ageRating` (string), `pageable` (Pageable object, **required**)
        
- **Chi tiết phim:** `GET /api/v1/public/movies/{id}`
    
- **Danh sách thể loại:** `GET /api/v1/public/genres/all`
    

## 3. Rạp & Phòng chiếu (Cinemas & Rooms)

### Quản lý Rạp (Admin)

- **Tạo rạp:** `POST /api/v1/admin/cinemas`
    
    - Body: `name` (string, **required**), `address` (string, **required**), `hotline` (string)
        
- **Cập nhật rạp:** `PUT /api/v1/admin/cinemas/{cinemaId}`
    
    - Body: `cinemaId` (int64, **required**), `name`, `address`, `hotline`
        
- **Xóa rạp:** `DELETE /api/v1/admin/cinemas/{cinemaId}`
    
- **Danh sách rạp (Public):** `GET /api/v1/public/cinemas`
    

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
    
- **Theo rạp:** `GET /api/v1/public/showtimes/cinemas/{cinemaId}` (Query: `date` format: date, **required**)
    

## 5. Ghế & Đặt vé (Seats & Bookings)

### Quản lý Ghế

- **Tạo sơ đồ ghế (Admin):** `POST /api/v1/admin/seats/create`
    
    - Body: Mảng các object: `roomId` (int64), `row` (number, > 0), `col` (number, > 0), `seatTypeId` (int64)
        
- **Xóa ghế theo phòng:** `DELETE /api/v1/admin/seats/delete/{roomId}`
    
- **Lấy ghế theo lịch chiếu (Public):** `GET /api/v1/public/seats` (Query: `showtimeId`, **required**)
    

### Giữ ghế (Locking)

- **Khóa ghế:** `POST /api/v1/seats/{seatId}/lock` (Query: `showtimeId`)
    
- **Mở khóa ghế:** `DELETE /api/v1/seats/{seatId}/unlock` (Query: `showtimeId`)
    
- **Khóa hàng loạt:** `POST /api/v1/seats/pre-lock`
    
    - Body: `showtimeId` (int64), `seatIds` (array of int64)
        

### Đặt vé & Thanh toán

- **Tạo Booking:** `POST /api/v1/bookings`
    
    - Body: `showtimeId` (**req**), `seatIds` (array, **req**), `paymentMethod` (enum: CASH, VNPAY, MOMO, **req**), `voucherId` (int64), `foodItems` (mảng object: `foodId`, `quantity` min 1)
        
- **Xác nhận thanh toán:** `POST /api/v1/bookings/{bookingId}/confirm-payment`
    
    - Body: `paymentRef` (string, **req**), `gateway` (string, **req**)
        

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
    

## 7. Cấu hình hệ thống (Admin)

### Cấu hình giá (Price Config)

- **Lấy tất cả:** `GET /api/v1/admin/price-config/all`
    
- **Cập nhật:** `POST /api/v1/admin/price-config/update`
    
    - Body: `configId` (**req**), `multiplier` (number, **req**), `dayType` (WEEKDAY, WEEKEND, HOLIDAY), `timeSlot` (MORNING, AFTERNOON, EVENING), `movieFormat` (2D, 3D, IMAX)
        

### Quản lý ngày lễ (Holiday)

- **Lấy tất cả:** `GET /api/v1/admin/holiday/all`
    
- **Tạo:** `POST /api/v1/admin/holiday/create`
    
    - Body: `date` (format: date, **req**), `name` (**req**), `isRecurring` (boolean, **req**)
        
- **Cập nhật:** `POST /api/v1/admin/holiday/update` (Thêm `holidayId`)
    
- **Xóa:** `DELETE /api/v1/admin/holiday/delete` (Body: `holidayId`)
    

### Khác

- **Đồ ăn:** `GET /api/v1/user/food` (Lấy danh sách đồ ăn)

## 8. Báo cáo & Thống kê (Reports - Admin Only)

### Tổng quan kinh doanh
- **Endpoint:** `GET /api/v1/admin/reports/overview`
- **Query Params:**
    - `from` (string, format: date, **required**) - Ngày bắt đầu (yyyy-MM-dd)
    - `to` (string, format: date, **required**) - Ngày kết thúc (yyyy-MM-dd)
- **Mô tả:** Trả về tổng doanh thu, doanh thu vé, doanh thu đồ ăn, số vé bán, AOV... trong khoảng thời gian.
- **Giới hạn:** Tối đa 366 ngày.

### Chuỗi doanh thu theo ngày
- **Endpoint:** `GET /api/v1/admin/reports/revenue/daily`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về doanh thu và số lượng bán hàng của từng ngày để vẽ biểu đồ.

### Doanh thu theo phim
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-movie`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách phim bán chạy kèm doanh thu, sắp xếp giảm dần.

### Doanh thu theo rạp
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-cinema`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách rạp kèm doanh thu, sắp xếp giảm dần.