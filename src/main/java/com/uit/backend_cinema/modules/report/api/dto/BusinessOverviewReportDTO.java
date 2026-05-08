package com.uit.backend_cinema.modules.report.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tổng quan kinh doanh trong khoảng thời gian truy vấn.
 */
@Data
@Builder
@Schema(description = "Tổng quan kinh doanh (doanh thu, vé, đồ ăn, AOV,...)")
public class BusinessOverviewReportDTO {

    @Schema(description = "Ngày bắt đầu khoảng báo cáo (inclusive)", example = "2025-01-01")
    private LocalDate from;

    @Schema(description = "Ngày kết thúc khoảng báo cáo (inclusive)", example = "2025-01-31")
    private LocalDate to;

    @Schema(description = "Tổng doanh thu (bookings.total_price, trạng thái PAID/CHECKED_IN)", example = "12500000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Doanh thu từ vé (sum tickets.price)", example = "10000000.00")
    private BigDecimal ticketRevenue;

    @Schema(description = "Doanh thu từ đồ ăn (sum food_orders.total_price)", example = "2500000.00")
    private BigDecimal foodRevenue;

    @Schema(description = "Tổng số vé đã bán", example = "450")
    private Long totalTicketsSold;

    @Schema(description = "Số booking đã thanh toán (PAID + CHECKED_IN)", example = "120")
    private Long totalBookingsPaid;

    @Schema(description = "Tổng giảm giá đã áp dụng (bookings.discount_amount)", example = "500000.00")
    private BigDecimal totalDiscount;

    @Schema(description = "Giá trị trung bình mỗi booking (AOV = totalRevenue / totalBookingsPaid)", example = "104166.67")
    private BigDecimal averageOrderValue;
}
