package com.uit.backend_cinema.modules.report.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.report.api.dto.BusinessOverviewReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.CinemaRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.DailyRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.MovieRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.mapper.ReportApiMapper;
import com.uit.backend_cinema.modules.report.domain.model.BusinessOverview;
import com.uit.backend_cinema.modules.report.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    static final int MAX_DAYS = 366;

    private final ReportRepository reportRepository;
    private final ReportApiMapper reportApiMapper;

    public BusinessOverviewReportDTO getOverview(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        BusinessOverview model = reportRepository.findOverview(dtFrom, dtTo)
                .orElse(new BusinessOverview());

        // Fill calculated and default fields
        model.setFrom(from);
        model.setTo(to);
        model.setTotalRevenue(getOrZero(model.getTotalRevenue()));
        model.setTicketRevenue(getOrZero(model.getTicketRevenue()));
        model.setFoodRevenue(getOrZero(model.getFoodRevenue()));
        model.setTotalTicketsSold(nullToZeroLong(model.getTotalTicketsSold()));
        model.setTotalBookingsPaid(nullToZeroLong(model.getTotalBookingsPaid()));
        model.setTotalDiscount(getOrZero(model.getTotalDiscount()));
        model.setAverageOrderValue(calcAov(model.getTotalRevenue(), model.getTotalBookingsPaid()));

        return reportApiMapper.toDTO(model);
    }

    public List<DailyRevenueReportDTO> getDailyRevenue(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        return reportRepository.findDailyRevenue(dtFrom, dtTo).stream()
                .peek(r -> {
                    r.setRevenue(getOrZero(r.getRevenue()));
                    r.setBookingCount(nullToZeroLong(r.getBookingCount()));
                    r.setTicketCount(nullToZeroLong(r.getTicketCount()));
                })
                .map(reportApiMapper::toDTO)
                .toList();
    }

    public List<MovieRevenueReportDTO> getRevenueByMovie(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        return reportRepository.findRevenueByMovie(dtFrom, dtTo).stream()
                .peek(r -> {
                    r.setTicketRevenue(getOrZero(r.getTicketRevenue()));
                    r.setTicketsSold(nullToZeroLong(r.getTicketsSold()));
                    r.setBookingCount(nullToZeroLong(r.getBookingCount()));
                })
                .map(reportApiMapper::toDTO)
                .toList();
    }

    public List<CinemaRevenueReportDTO> getRevenueByCinema(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        return reportRepository.findRevenueByCinema(dtFrom, dtTo).stream()
                .peek(r -> {
                    r.setTicketRevenue(getOrZero(r.getTicketRevenue()));
                    r.setTicketsSold(nullToZeroLong(r.getTicketsSold()));
                    r.setBookingCount(nullToZeroLong(r.getBookingCount()));
                })
                .map(reportApiMapper::toDTO)
                .toList();
    }

    void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(
                    "Tham số 'from' và 'to' là bắt buộc",
                    ErrorCode.VALIDATION_FAILED);
        }
        if (from.isAfter(to)) {
            throw new BusinessException(
                    "Ngày bắt đầu (from) không được lớn hơn ngày kết thúc (to)",
                    ErrorCode.VALIDATION_FAILED);
        }
        long days = to.toEpochDay() - from.toEpochDay();
        if (days >= MAX_DAYS) {
            throw new BusinessException(
                    "Khoảng thời gian truy vấn tối đa là " + MAX_DAYS + " ngày",
                    ErrorCode.VALIDATION_FAILED);
        }
    }

    private BigDecimal getOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nullToZeroLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal calcAov(BigDecimal totalRevenue, Long totalBookingsPaid) {
        if (totalBookingsPaid == null || totalBookingsPaid == 0L) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(BigDecimal.valueOf(totalBookingsPaid), 2, RoundingMode.HALF_UP);
    }
}
