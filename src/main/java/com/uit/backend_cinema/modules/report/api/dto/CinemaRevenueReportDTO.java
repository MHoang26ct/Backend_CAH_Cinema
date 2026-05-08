package com.uit.backend_cinema.modules.report.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Doanh thu và số liệu bán vé theo rạp chiếu")
public class CinemaRevenueReportDTO {

    @Schema(description = "ID của rạp", example = "3")
    private Long cinemaId;

    @Schema(description = "Tên rạp", example = "CGV Vincom Center")
    private String cinemaName;

    @Schema(description = "Doanh thu vé tại rạp này", example = "8200000.00")
    private BigDecimal ticketRevenue;

    @Schema(description = "Số vé đã bán tại rạp", example = "310")
    private Long ticketsSold;

    @Schema(description = "Số booking tại rạp", example = "98")
    private Long bookingCount;
}
