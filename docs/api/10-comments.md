# 10. Bình luận phim

## Public

| Method | Endpoint | Query |
| --- | --- | --- |
| `GET` | `/api/v1/public/comments/movies/{movieId}` | `page` mặc định `0`, `size` mặc định `3` |

Kết quả là `Slice<CommentResponse>` được bọc trong `ApiResponse`, sắp xếp `createdAt,DESC`.

## User đã đăng nhập

| Method | Endpoint | Request |
| --- | --- | --- |
| `POST` | `/api/v1/comments/movies/{movieId}` | body `content` không được rỗng |
| `DELETE` | `/api/v1/comments/{commentId}` | — |

Backend chỉ cho phép tạo bình luận khi người dùng đủ điều kiện nghiệp vụ và chỉ tác giả mới được xóa bình luận của mình. Các endpoint trả `ApiResponse`; dữ liệu bình luận gồm `commentId`, `userId`, `userName`, `userAvatar`, `content`, `createdAt`.
