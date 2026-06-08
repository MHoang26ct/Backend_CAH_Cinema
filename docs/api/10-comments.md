# 10. Bình luận phim (Movie Comments)

### Quy tắc & Điều kiện:
- Chỉ cho phép người dùng đã xem phim (tức là trạng thái booking của phim đó là `CHECKED_IN`) và số lần comment không được lớn hơn số lần xem phim.
- Chỉ người tạo bình luận mới được quyền xóa bình luận của mình.

## Dành cho Public (Không yêu cầu đăng nhập)

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

## Dành cho User (Yêu cầu đăng nhập)

- **Thêm bình luận cho phim:** `POST /api/v1/comments/movies/{movieId}`
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

- **Xóa bình luận:** `DELETE /api/v1/comments/{commentId}`
    - Response: `200 OK` (bọc trong `ApiResponse`)
    
```json
{
  "code": 200,
  "message": "Xóa bình luận thành công",
  "data": null
}
```
