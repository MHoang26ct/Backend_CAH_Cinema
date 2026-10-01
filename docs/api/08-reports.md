# 8. Báo cáo & thống kê

Tất cả endpoint yêu cầu `ROLE_ADMIN` và đều nhận hai query bắt buộc:

- `from`: ngày bắt đầu, định dạng `yyyy-MM-dd`
- `to`: ngày kết thúc, định dạng `yyyy-MM-dd`

Khoảng ngày tối đa 366 ngày.

| Method | Endpoint | `ApiResponse.data` |
| --- | --- | --- |
| `GET` | `/api/v1/admin/reports/overview` | `BusinessOverviewReportDTO` |
| `GET` | `/api/v1/admin/reports/revenue/daily` | mảng `DailyRevenueReportDTO` |
| `GET` | `/api/v1/admin/reports/revenue/by-movie` | mảng `MovieRevenueReportDTO` |
| `GET` | `/api/v1/admin/reports/revenue/by-cinema` | mảng `CinemaRevenueReportDTO` |

`overview` trả các chỉ số tổng hợp như doanh thu, doanh thu vé/đồ ăn, số vé, số booking, tổng giảm giá và AOV. Ba endpoint còn lại trả dữ liệu theo ngày, phim hoặc rạp để phục vụ biểu đồ/bảng thống kê.
