# Backend Cinema API

Backend cho hệ thống quản lý rạp chiếu phim, đặt vé trực tuyến và vận hành khuyến mãi/báo cáo.
Dự án được xây dựng bằng Spring Boot theo hướng module hóa, tách rõ `api` - `domain` - `infrastructure` để dễ mở rộng và bảo trì.

## 1. Tổng quan

Backend cung cấp các nhóm chức năng chính:

- Xác thực người dùng (email/password, Google OAuth, OTP, refresh token)
- Quản lý phim, thể loại, rạp, phòng chiếu, lịch chiếu
- Quản lý ghế, giữ ghế tạm thời bằng Redis
- Đặt vé, xác nhận thanh toán, tạo hóa đơn/ticket
- Voucher, cấu hình giá linh hoạt theo ngày/khung giờ/định dạng phim
- Quản lý đồ ăn kèm theo booking
- Báo cáo doanh thu và chỉ số kinh doanh
- Gửi email và xử lý tác vụ bất đồng bộ theo cơ chế outbox

## 2. Công nghệ sử dụng

- Java 17
- Spring Boot 3.2.5
- Spring Security + JWT
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- Spring Mail (Gmail SMTP)
- MapStruct
- Lombok
- OpenAPI/Swagger (springdoc)
- JUnit 5 + Spring Test + Testcontainers
- Gradle

## 3. Kiến trúc & cấu trúc thư mục

Dự án được tổ chức theo module nghiệp vụ, mỗi module thường có các lớp:

- `api`: controller, request/response DTO, mapper giao tiếp HTTP
- `domain`: entity nghiệp vụ, service, repository interface
- `infrastructure`: JPA entity/repository, mapper, persistence implementation

Cấu trúc chính:

```text
src/main/java/com/uit/backend_cinema
├── common
│   ├── doc
│   ├── exception
│   ├── sercurity
│   └── util
└── modules
    ├── auth
    ├── booking
    ├── cinema
    ├── food_order
    ├── invoice
    ├── movies
    ├── notification
    ├── outbox
    ├── price_config
    ├── report
    ├── seat
    ├── showtime
    ├── ticket
    └── voucher
```

## 4. Yêu cầu môi trường

- JDK 17+
- Gradle (hoặc dùng `./gradlew`)
- PostgreSQL 15+
- Redis 7+
- Docker & Docker Compose (khuyến nghị để dựng nhanh DB/Redis)

## 5. Cấu hình biến môi trường

Dự án đọc biến môi trường từ file `.env` và sử dụng trong `application.yml`.

Tạo/cập nhật file `.env` tại thư mục gốc với các biến:

```env
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
JWT_SECRET=your_long_jwt_secret
DB_URL=jdbc:postgresql://localhost:5432/cinema_db
DB_USER=postgres
DB_PASSWORD=postgres
REDIS_PORT=6379
GMAIL_USERNAME=your_email@gmail.com
GG_APP_PASSWORD=your_gmail_app_password
```

Lưu ý:

- Không commit thông tin nhạy cảm (client secret, JWT secret, app password).
- `DB_URL` cần trỏ đúng host PostgreSQL theo môi trường chạy (local/dev/staging).

## 6. Khởi chạy dự án

### Cách 1: Chạy PostgreSQL + Redis bằng Docker

1. Khởi động dịch vụ:

```bash
docker compose up -d
```

2. Import schema và dữ liệu mẫu (nếu cần):

```bash
psql -h localhost -U <db_user> -d cinema_db -f cah_cinema.sql
psql -h localhost -U <db_user> -d cinema_db -f seed_background_data.sql
```

3. Chạy ứng dụng:

```bash
./gradlew bootRun
```

Ứng dụng mặc định chạy tại `http://localhost:8080`.

### Cách 2: Chạy trực tiếp bằng hạ tầng sẵn có

1. Chuẩn bị PostgreSQL/Redis thủ công.
2. Cập nhật `.env` tương ứng.
3. Chạy:

```bash
./gradlew bootRun
```

## 7. Build và test

Build project:

```bash
./gradlew clean build
```

Chạy test:

```bash
./gradlew test
```

*Lưu ý khi chạy test:*
- Dự án sử dụng **Testcontainers** để khởi chạy một cơ sở dữ liệu PostgreSQL thực tế trong container Docker phục vụ kiểm thử tích hợp. Do đó, bạn cần khởi động **Docker Daemon** (ví dụ: Docker Desktop) trên máy của mình trước khi chạy test.
- Các thiết lập cấu hình của môi trường test được quản lý tại file [application-test.yml](file:///Users/mhoang26ct/My%20Project/backend_cinema/src/test/resources/application-test.yml).

## 8. Tài liệu API

- Tài liệu endpoint tổng hợp: [api-docs.md](./api-docs.md)
- Swagger UI (khi app đang chạy):
  - `http://localhost:8080/swagger-ui/index.html`

Một số nhóm API chính:

- `Auth`: đăng ký, đăng nhập, OTP, refresh token
- `Movies/Genres`: quản lý và tra cứu phim
- `Cinema/Room/Showtime`: vận hành rạp và lịch chiếu
- `Seat/Booking`: giữ ghế, tạo booking, xác nhận thanh toán
- `Voucher/Price Config/Holiday`: khuyến mãi và định giá
- `Report`: thống kê doanh thu cho admin

## 9. Bảo mật & phân quyền

- Sử dụng JWT cho xác thực phiên đăng nhập.
- Controller được phân tách nhóm `public`, `user`, `admin`.
- Cấu hình Security nằm tại:
  - `src/main/java/com/uit/backend_cinema/common/sercurity/SecurityConfig.java`
  - `src/main/java/com/uit/backend_cinema/common/sercurity/JwtAuthenticationFilter.java`

## 10. Gợi ý quy trình phát triển

1. Tạo branch tính năng từ `main`.
2. Cập nhật schema/dữ liệu mẫu nếu thay đổi nghiệp vụ.
3. Viết test cho domain service/repository quan trọng.
4. Chạy `./gradlew test` trước khi tạo pull request.
5. Cập nhật `api-docs.md` khi endpoint thay đổi.

## 11. Triển khai

### 11.1. Triển khai thủ công
Project có sẵn script và biến deploy:

- Script: `deploy.sh`
- Biến môi trường deploy: `deploy.env`

Bạn nên rà soát và thay thế các thông số server trước khi dùng cho production.

### 11.2. Triển khai tự động (CI/CD GitHub Actions)
Dự án được tích hợp sẵn đường ống dẫn tự động hóa trong thư mục [.github/workflows/](file:///Users/mhoang26ct/My%20Project/backend_cinema/.github/workflows/):

- **CI Pipeline ([ci.yml](file:///Users/mhoang26ct/My%20Project/backend_cinema/.github/workflows/ci.yml))**:
  - Tự động kích hoạt khi có `push`/`pull_request` trên nhánh `main` và `develop`.
  - Cài đặt JDK 17, thiết lập cache cho Gradle, build project và chạy toàn bộ suite kiểm thử (đã tích hợp Testcontainers).
  - Đẩy lên Test Report (dạng HTML) và đóng gói lưu trữ file JAR kết quả nếu build thành công trên nhánh `main`.
- **CD Pipeline ([deploy.yml](file:///Users/mhoang26ct/My%20Project/backend_cinema/.github/workflows/deploy.yml))**:
  - Tự động chạy sau khi CI Pipeline hoàn thành xuất sắc trên nhánh `main`.
  - Tải file JAR đã build được và thực hiện triển khai qua SSH lên máy chủ đích sử dụng `appleboy/ssh-action`.
  - Để chạy thực tế, bạn cần cấu hình các biến Secrets trong GitHub Repository: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`.

## 12. Đóng góp

Mọi đóng góp cải tiến đều được hoan nghênh. Khi gửi PR, vui lòng:

- Mô tả rõ mục tiêu thay đổi
- Đính kèm hướng kiểm thử
- Cập nhật tài liệu liên quan (README/API docs) nếu cần

## 13. Contributors

- [MHoang26ct](https://github.com/MHoang26ct)
- [LeVanAnUITK19](https://github.com/LeVanAnUITK19)
