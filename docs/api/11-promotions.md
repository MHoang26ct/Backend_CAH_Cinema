# 11. Bài viết khuyến mãi

## Public

| Method | Endpoint | Query |
| --- | --- | --- |
| `GET` | `/api/v1/public/promotions` | Spring pagination: `page`, `size`, `sort`; mặc định `size=9`, `createdAt,DESC` |
| `GET` | `/api/v1/public/promotions/{id}` | — |

Chỉ bài viết active được hiển thị. Response được bọc trong `ApiResponse`:

- Danh sách: `Page<PromotionArticlePreviewResponse>`.
- Chi tiết: `PromotionArticleResponse`.

## Admin — yêu cầu `ROLE_ADMIN`

| Method | Endpoint | Request / response |
| --- | --- | --- |
| `GET` | `/api/v1/admin/promotions` | query `page` mặc định `0`, `size` mặc định `10`; trả trực tiếp `Page<PromotionArticlePreviewResponse>` |
| `GET` | `/api/v1/admin/promotions/{id}` | trả trực tiếp `PromotionArticleResponse` |
| `POST` | `/api/v1/admin/promotions` | `PromotionArticleRequest`; trả `201 Created` và body trực tiếp |
| `PUT` | `/api/v1/admin/promotions/{id}` | `PromotionArticleRequest`; trả `200 OK` và body trực tiếp |
| `DELETE` | `/api/v1/admin/promotions/{id}` | trả `204 No Content` |

`PromotionArticleRequest` có `title` và `shortDescription` bắt buộc; `startDate`, `endDate`, `conditions`, `imageUrl`, `note`, `isActive` là tùy chọn. Ngày dùng định dạng `yyyy-MM-dd`.

Preview gồm `promotionId`, `title`, `shortDescription`, `imageUrl`, `createdAt`, `isActive`; endpoint chi tiết trả thêm nội dung và metadata bài viết.
