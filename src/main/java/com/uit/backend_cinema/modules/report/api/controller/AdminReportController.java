package com.uit.backend_cinema.modules.report.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.report.api.dto.BusinessOverviewReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.CinemaRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.DailyRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.MovieRevenueReportDTO;
import com.uit.backend_cinema.modules.report.domain.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "Admin Reports", description = "Các API báo cáo doanh thu dành riêng cho Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "Lấy tổng quan báo cáo kinh doanh",
            description = """
                    Trả về tổng doanh thu, vé bán, AOV... trong khoảng thời gian.
                    - Tính doanh thu từ booking có status PAID, CHECKED_IN.
                    - Khoảng thời gian tối đa: 366 ngày.
                    - Timezone: UTC+7. Ngày 'to' là inclusive.
                    """
    )
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<BusinessOverviewReportDTO>> getOverview(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        BusinessOverviewReportDTO data = reportService.getOverview(from, to);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy báo cáo tổng quan thành công"));
    }

    @Operation(
            summary = "Lấy chuỗi doanh thu theo từng ngày",
            description = "Trả về doanh thu và số lượng bán hàng của từng ngày trong khoảng thời gian để vẽ biểu đồ."
    )
    @GetMapping("/revenue/daily")
    public ResponseEntity<ApiResponse<List<DailyRevenueReportDTO>>> getDailyRevenue(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DailyRevenueReportDTO> data = reportService.getDailyRevenue(from, to);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy báo cáo doanh thu ngày thành công"));
    }

    @Operation(
            summary = "Lấy báo cáo doanh thu theo phim",
            description = "Trả về danh sách phim bán chạy kèm doanh thu, sắp xếp giảm dần theo doanh thu."
    )
    @GetMapping("/revenue/by-movie")
    public ResponseEntity<ApiResponse<List<MovieRevenueReportDTO>>> getRevenueByMovie(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<MovieRevenueReportDTO> data = reportService.getRevenueByMovie(from, to);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy báo cáo doanh thu theo phim thành công"));
    }

    @Operation(
            summary = "Lấy báo cáo doanh thu theo rạp",
            description = "Trả về danh sách rạp kèm doanh thu, sắp xếp giảm dần theo doanh thu."
    )
    @GetMapping("/revenue/by-cinema")
    public ResponseEntity<ApiResponse<List<CinemaRevenueReportDTO>>> getRevenueByCinema(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<CinemaRevenueReportDTO> data = reportService.getRevenueByCinema(from, to);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy báo cáo doanh thu theo rạp thành công"));
    }
}
