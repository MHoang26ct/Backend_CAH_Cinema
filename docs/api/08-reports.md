# 8. Báo cáo & Thống kê (Reports)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

### Tổng quan kinh doanh
- **Endpoint:** `GET /api/v1/admin/reports/overview`
- **Query Params:**
    - `from` (string, format: date, **required**) - Ngày bắt đầu (yyyy-MM-dd)
    - `to` (string, format: date, **required**) - Ngày kết thúc (yyyy-MM-dd)
- **Mô tả:** Trả về tổng doanh thu, doanh thu vé, doanh thu đồ ăn, số vé bán, AOV... trong khoảng thời gian.
- **Giới hạn:** Tối đa 366 ngày.

```json
{
  "code": 200,
  "data": {
    "from": "2026-05-01",
    "to": "2026-05-18",
    "totalRevenue": 12500000.00,
    "ticketRevenue": 10000000.00,
    "foodRevenue": 2500000.00,
    "totalTicketsSold": 450,
    "totalBookingsPaid": 120,
    "totalDiscount": 500000.00,
    "averageOrderValue": 104166.67
  }
}
```

### Chuỗi doanh thu theo ngày
- **Endpoint:** `GET /api/v1/admin/reports/revenue/daily`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về doanh thu và số lượng bán hàng của từng ngày để vẽ biểu đồ.

```json
{
  "code": 200,
  "data": [
    {
      "date": "2026-05-15",
      "revenue": 3500000.00,
      "bookingCount": 18,
      "ticketCount": 45
    }
  ]
}
```

### Doanh thu theo phim
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-movie`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách phim bán chạy kèm doanh thu, sắp xếp giảm dần.

```json
{
  "code": 200,
  "data": [
    {
      "movieId": 3,
      "movieTitle": "Avengers: Endgame",
      "ticketRevenue": 5600000.00,
      "ticketsSold": 210,
      "bookingCount": 72
    }
  ]
}
```

### Doanh thu theo rạp
- **Endpoint:** `GET /api/v1/admin/reports/revenue/by-cinema`
- **Query Params:**
    - `from` (string, format: date, **required**)
    - `to` (string, format: date, **required**)
- **Mô tả:** Trả về danh sách rạp kèm doanh thu, sắp xếp giảm dần.

```json
{
  "code": 200,
  "data": [
    {
      "cinemaId": 1,
      "cinemaName": "CGV Vincom Center",
      "ticketRevenue": 8200000.00,
      "ticketsSold": 310,
      "bookingCount": 98
    }
  ]
}
```
