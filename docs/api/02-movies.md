# 2. Phim & Thể loại

## Admin — yêu cầu `ROLE_ADMIN`

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/admin/movies/create` | `UpdateOrCreateMovieDTO` |
| `PUT` | `/api/v1/admin/movies/update/{id}` | `UpdateOrCreateMovieDTO` |
| `DELETE` | `/api/v1/admin/movies/delete/{id}` | — |

Request phim hỗ trợ `title`, `description`, `duration`, `releaseDate`, `ageRating`, `posterUrl`, `trailerUrl`, `directorName`, `actorList`, `genreIdList`. `duration` nếu có phải từ 15 phút; `genreIdList` không được rỗng. Tạo/cập nhật/xóa trả `ApiResponse<MovieDetailDTO>`.

## Public

| Method | Endpoint | Query |
| --- | --- | --- |
| `GET` | `/api/v1/public/movies` | `title`, `genreId`, `ageRating` tùy chọn; phân trang `page`, `size`, `sort` |
| `GET` | `/api/v1/public/movies/featured` | — |
| `GET` | `/api/v1/public/movies/{id}` | — |
| `GET` | `/api/v1/public/genres/all` | — |

- Danh sách phim mặc định `size=10`, sort `releaseDate,DESC`.
- `featured` trả object có hai mảng `nowShowing` và `upcoming`; mỗi nhóm tối đa 5 phim.
- Chi tiết phim trả `MovieDetailDTO`, gồm thông tin phim và `genres`.
- Toàn bộ endpoint public trong trang này trả `ApiResponse`.
