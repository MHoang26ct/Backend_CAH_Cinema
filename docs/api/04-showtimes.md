# 4. Lịch chiếu (Showtimes)

## Admin — yêu cầu `ROLE_ADMIN`

| Method | Endpoint | Request / query |
| --- | --- | --- |
| `POST` | `/api/v1/admin/showtime` | `movieId`, `roomId`, `format`, `startTime`, `basePrice` bắt buộc |
| `PUT` | `/api/v1/admin/showtime` | `showtimeId`, `movieId`, `roomId`, `startTime`, `basePrice`, `status` bắt buộc; `format` tùy chọn |
| `DELETE` | `/api/v1/admin/showtime/{showtimeId}` | — |
| `GET` | `/api/v1/admin/showtime/rooms/{roomId}` | query `date` bắt buộc, định dạng `yyyy-MM-dd` |
| `POST` | `/api/v1/admin/showtime/cancel-by-room` | `roomId`, `fromDate`, `toDate` bắt buộc; `reason` tùy chọn |

### Lưu ý thời gian

- `endTime` vẫn được chấp nhận để tương thích request cũ nhưng **không bắt buộc và không được dùng để tính lịch chiếu**.
- Backend tính `endTime` từ `startTime` và snapshot thời lượng của phim.
- Khi cập nhật, thời lượng snapshot của suất chiếu được giữ khi chỉ đổi giờ; nếu đổi phim, backend dùng thời lượng của phim mới.
- `date`, `fromDate`, `toDate` dùng định dạng `yyyy-MM-dd`; `startTime` và `endTime` trong response dùng định dạng date-time.

## Public

| Method | Endpoint | Query bắt buộc |
| --- | --- | --- |
| `GET` | `/api/v1/public/showtimes/movies/{movieId}` | `date` (`yyyy-MM-dd`) |
| `GET` | `/api/v1/public/showtimes/cinemas/{cinemaId}` | `date` (`yyyy-MM-dd`) |

Các endpoint public chỉ trả lịch chiếu phù hợp để đặt vé. Response được bọc trong `ApiResponse`.
