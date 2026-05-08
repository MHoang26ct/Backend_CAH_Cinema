package com.uit.backend_cinema.modules.report.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Doanh thu của một ngày trong chuói báo cáo theo ngày")
public class DailyRevenueReportDTO {

    @Schema(description = "Ngày báo cáo", example = "2025-01-15")
    private LocalDate date;

    @Schema(description = "Tổng doanh thu ngày đó", example = "3500000.00")
    private BigDecimal revenue;

    @Schema(description = "Số booking thanh toán trong ngày", example = "18")
    private Long bookingCount;

    @Schema(description = "Số vé bán trong ngày", example = "45")
    private Long ticketCount;
}
