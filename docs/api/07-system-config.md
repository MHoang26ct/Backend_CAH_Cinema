# 7. Cấu hình hệ thống

Tất cả endpoint trong tài liệu này yêu cầu `ROLE_ADMIN`, trừ `GET /api/v1/foods` yêu cầu người dùng đã đăng nhập.

## Cấu hình giá

| Method | Endpoint | Request |
| --- | --- | --- |
| `GET` | `/api/v1/admin/price-config/all` | — |
| `POST` | `/api/v1/admin/price-config/update` | `configId`, `multiplier` bắt buộc; `dayType`, `timeSlot`, `movieFormat` tùy chọn |

`multiplier` phải dương, tối đa 3 chữ số phần nguyên và 2 chữ số phần thập phân. Response cấu hình giá gồm `configId`, `dayType`, `timeSlot`, `movieFormat`, `multiplier`.

## Ngày lễ

| Method | Endpoint | Request |
| --- | --- | --- |
| `GET` | `/api/v1/admin/holiday/all` | — |
| `POST` | `/api/v1/admin/holiday/create` | `date`, `name`, `isRecurring` bắt buộc |
| `POST` | `/api/v1/admin/holiday/update` | `holidayId`, `date`, `name`, `isRecurring` bắt buộc |
| `DELETE` | `/api/v1/admin/holiday/delete` | body `{ "holidayId": 1 }` |

`date` dùng định dạng `yyyy-MM-dd`. Response ngày lễ gồm `holidayId`, `date`, `name`, `isRecurring`.

## Đồ ăn

| Method | Endpoint | Quyền | Request |
| --- | --- | --- | --- |
| `GET` | `/api/v1/admin/food` | Admin | — |
| `POST` | `/api/v1/admin/food` | Admin | `name`, `price`, `category` bắt buộc; `description`, `imageUrl`, `available` tùy chọn |
| `PUT` | `/api/v1/admin/food/{id}` | Admin | Body giống tạo mới |
| `DELETE` | `/api/v1/admin/food/{id}` | Admin | — |
| `GET` | `/api/v1/foods` | JWT hợp lệ | Chỉ trả món đang available |

`price` phải lớn hơn 0. Tạo đồ ăn trả `201 Created`; các endpoint food còn lại trả `200 OK`, bọc trong `ApiResponse`.
