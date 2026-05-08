package com.uit.backend_cinema.modules.report.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Doanh thu và số liệu bán vé theo phim")
public class MovieRevenueReportDTO {

    @Schema(description = "ID của phim", example = "7")
    private Long movieId;

    @Schema(description = "Tên phim", example = "Avengers: Endgame")
    private String movieTitle;

    @Schema(description = "Doanh thu vé của phim này", example = "5600000.00")
    private BigDecimal ticketRevenue;

    @Schema(description = "Số vé đã bán", example = "210")
    private Long ticketsSold;

    @Schema(description = "Số booking liên quan", example = "72")
    private Long bookingCount;
}
