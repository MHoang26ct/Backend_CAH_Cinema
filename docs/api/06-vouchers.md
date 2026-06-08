# 6. Voucher & Khuyến mãi (Vouchers)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

- **Danh sách voucher:** `GET /api/v1/admin/vouchers` (Query: `pageable`)
    
- **Tạo voucher:** `POST /api/v1/admin/vouchers/create`
    
    - Body: `code`, `type` (FIXED_AMOUNT, PERCENT), `value`, `quantity`, `startAt`, `expiredAt` (**Tất cả required**)
        
- **Cập nhật voucher:** `POST /api/v1/admin/vouchers/update`
    
    - Thêm: `voucherId`, `isActive`, `isDeleted`, `minOrderValue`, `maxDiscount`.
        
- **Xóa voucher:** `DELETE /api/v1/admin/vouchers/{voucherId}`
    

## Dành cho User (Yêu cầu đăng nhập)

- **Lấy voucher của tôi:** `GET /api/v1/vouchers`

```json
{
  "code": 200,
  "data": [
    {
      "type": "PERCENT",
      "value": 10.00,
      "maxDiscount": 50000.00,
      "minOrderValue": 200000.00,
      "quantity": 100,
      "usedCount": 23,
      "startAt": "2026-05-01T00:00:00",
      "expiredAt": "2026-05-31T23:59:59"
    }
  ]
}
```
