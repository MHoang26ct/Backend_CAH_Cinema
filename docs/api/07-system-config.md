# 7. Cấu hình hệ thống (System Config)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

### Cấu hình giá (Price Config)

- **Lấy tất cả:** `GET /api/v1/admin/price-config/all`

```json
{
  "code": 200,
  "data": [
    {
      "configId": 1,
      "dayType": "WEEKEND",
      "timeSlot": "EVENING",
      "movieFormat": "3D",
      "multiplier": 1.5
    }
  ]
}
```

- **Cập nhật:** `POST /api/v1/admin/price-config/update`
    
    - Body: `configId` (**req**), `multiplier` (number, **req**), `dayType` (WEEKDAY, WEEKEND, HOLIDAY), `timeSlot` (MORNING, AFTERNOON, EVENING), `movieFormat` (2D, 3D, IMAX)
        

### Quản lý ngày lễ (Holiday)

- **Lấy tất cả:** `GET /api/v1/admin/holiday/all`

```json
{
  "code": 200,
  "data": [
    {
      "holidayId": 1,
      "date": "2026-09-02",
      "name": "Quốc khánh",
      "isRecurring": true
    }
  ]
}
```
    
- **Tạo:** `POST /api/v1/admin/holiday/create`
    
    - Body: `date` (format: date, **req**), `name` (**req**), `isRecurring` (boolean, **req**)
        
- **Cập nhật:** `POST /api/v1/admin/holiday/update` (Thêm `holidayId`)
    
- **Xóa:** `DELETE /api/v1/admin/holiday/delete` (Body: `holidayId`)
    

### Đồ ăn (Food)

- **Danh sách tất cả đồ ăn:** `GET /api/v1/admin/food`
    - Trả về tất cả (bao gồm cả unavailable/deleted).

- **Tạo đồ ăn:** `POST /api/v1/admin/food`
    - Body:
        - `name` (string, **required**)
        - `description` (string)
        - `price` (number, **required**, > 0)
        - `category` (enum, **required**)
        - `imageUrl` (string)
        - `available` (boolean, default: true)

- **Cập nhật đồ ăn:** `PUT /api/v1/admin/food/{id}`
    - Body: giống tạo mới.

- **Xóa đồ ăn (soft delete):** `DELETE /api/v1/admin/food/{id}`

## Dành cho User / Public

### Đồ ăn (Food)

- **Danh sách đồ ăn (available):** `GET /api/v1/foods`
    - Chỉ trả về các món đang available.

```json
{
  "code": 200,
  "message": "Lấy danh sách thức ăn thành công",
  "data": [
    {
      "foodId": 1,
      "name": "Combo Bắp + Nước",
      "description": "Bắp rang bơ lớn + Pepsi lớn",
      "price": 75000.00,
      "category": "COMBO",
      "imageUrl": "https://...",
      "available": true
    }
  ]
}
```
