# 3. Rạp & Phòng chiếu (Cinemas & Rooms)

## Admin — yêu cầu `ROLE_ADMIN`

### Rạp

| Method | Endpoint | Request body |
| --- | --- | --- |
| `GET` | `/api/v1/admin/cinemas/{cinemaId}` | — |
| `POST` | `/api/v1/admin/cinemas` | `name`, `address` bắt buộc; `imageUrl`, `hotline` tùy chọn |
| `PUT` | `/api/v1/admin/cinemas/{cinemaId}` | `name`, `address` bắt buộc; `imageUrl`, `hotline` tùy chọn |
| `DELETE` | `/api/v1/admin/cinemas/{cinemaId}` | — |

`cinemaId` lấy từ path. Không cần gửi `cinemaId` trong body; controller sẽ gán ID từ path khi cập nhật.

### Phòng chiếu

| Method | Endpoint | Request body |
| --- | --- | --- |
| `GET` | `/api/v1/admin/cinemas/{cinemaId}/rooms` | — |
| `POST` | `/api/v1/admin/cinemas/{cinemaId}/rooms` | `roomName` bắt buộc |
| `PUT` | `/api/v1/admin/cinemas/rooms/{roomId}` | `roomName` bắt buộc |
| `DELETE` | `/api/v1/admin/cinemas/rooms/{roomId}` | — |

`cinemaId` và `roomId` tương ứng được lấy từ path, không cần đưa vào request body.

## Public

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/api/v1/public/cinemas` | Danh sách rạp |

Các endpoint trên trả `ApiResponse`; dữ liệu rạp có các trường `cinemaId`, `name`, `address`, `imageUrl`, `hotline`.
