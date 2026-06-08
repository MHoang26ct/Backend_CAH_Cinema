# 11. Bài viết khuyến mãi (Promotion Articles)

### Quy tắc hiển thị:
- **Xem danh sách/Preview:** Chỉ trả về thông tin cơ bản: `promotionId`, `title`, `shortDescription`, `imageUrl`, `createdAt`, `isActive`. Tránh tải các trường chi tiết dài như `startDate`, `endDate`, `conditions`, `note` để tối ưu tải trang.
- **Xem chi tiết:** Trả về đầy đủ tất cả các trường thông tin bài viết khuyến mãi.
- Các trường `startDate` và `endDate` (dạng `LocalDate`) chỉ lưu để hiển thị, không mang ý nghĩa logic nghiệp vụ.

## Dành cho Public (Không yêu cầu đăng nhập)

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

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

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
