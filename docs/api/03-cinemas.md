# 3. Rạp & Phòng chiếu (Cinemas & Rooms)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

### Quản lý Rạp

- **Tạo rạp:** `POST /api/v1/admin/cinemas`
    
    - Body: `name` (string, **required**), `address` (string, **required**), `imageUrl` (string), `hotline` (string)
        
- **Cập nhật rạp:** `PUT /api/v1/admin/cinemas/{cinemaId}`
    
    - Body: `cinemaId` (int64, **required**), `name`, `address`, `imageUrl`, `hotline`
        
- **Xóa rạp:** `DELETE /api/v1/admin/cinemas/{cinemaId}`

### Quản lý Phòng chiếu

- **Lấy danh sách phòng theo rạp:** `GET /api/v1/admin/cinemas/{cinemaId}/rooms`
    
- **Tạo phòng:** `POST /api/v1/admin/cinemas/{cinemaId}/rooms`
    
    - Body: `cinemaId` (int64, **required**), `roomName` (string, **required**)
        
- **Cập nhật phòng:** `PUT /api/v1/admin/cinemas/rooms/{roomId}`
    
    - Body: `roomId` (int64, **required**), `roomName` (string, **required**)
        
- **Xóa phòng:** `DELETE /api/v1/admin/cinemas/rooms/{roomId}`

## Dành cho Public (Không yêu cầu đăng nhập)

- **Danh sách rạp:** `GET /api/v1/public/cinemas`

```json
{
  "code": 200,
  "message": "Lấy danh sách rạp thành công",
  "data": [
    {
      "cinemaId": 1,
      "name": "CGV Vincom Bà Triệu",
      "address": "191 Bà Triệu, Hai Bà Trưng, Hà Nội",
      "imageUrl": "https://...",
      "hotline": "1900 6017"
    }
  ]
}
```
