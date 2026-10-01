# 6. Voucher

## Admin — yêu cầu `ROLE_ADMIN`

| Method | Endpoint | Request / query |
| --- | --- | --- |
| `GET` | `/api/v1/admin/vouchers` | phân trang Spring: `page`, `size`, `sort`; mặc định `size=20`, sort `voucherId,DESC` |
| `GET` | `/api/v1/admin/vouchers/{voucherId}` | — |
| `POST` | `/api/v1/admin/vouchers/create` | `code`, `type`, `value`, `quantity`, `startAt`, `expiredAt` bắt buộc; `maxDiscount`, `minOrderValue` tùy chọn |
| `POST` | `/api/v1/admin/vouchers/update` | `voucherId`, `code`, `type`, `value`, `minOrderValue`, `quantity`, `startAt`, `expiredAt`, `isActive`, `isDeleted` bắt buộc; `maxDiscount` tùy chọn |
| `DELETE` | `/api/v1/admin/vouchers/{voucherId}` | — |

`type` là enum voucher do backend định nghĩa (hiện gồm `FIXED_AMOUNT` và `PERCENT`). `startAt` và `expiredAt` dùng date-time. Voucher chỉ hiệu lực trong khoảng `[startAt, expiredAt)` và hai mốc phải khác nhau.

## User đã đăng nhập

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/api/v1/vouchers` | Danh sách voucher khả dụng của người dùng hiện tại |

Các endpoint voucher trả `ApiResponse`.
