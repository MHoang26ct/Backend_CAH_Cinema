package com.uit.backend_cinema.modules.report.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.report.api.dto.BusinessOverviewReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.CinemaRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.DailyRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.MovieRevenueReportDTO;
import com.uit.backend_cinema.modules.report.infrastructure.repository.ReportReadRepository;
import com.uit.backend_cinema.modules.report.infrastructure.repository.dto.CinemaRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.repository.dto.DailyRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.repository.dto.MovieRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.repository.dto.OverviewProjection;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReportService {

    static final int MAX_DAYS = 366;

    private final ReportReadRepository reportReadRepository;

    public ReportService(ReportReadRepository reportReadRepository) {
        this.reportReadRepository = reportReadRepository;
    }

    public BusinessOverviewReportDTO getOverview(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.plusDays(1).atStartOfDay();

        OverviewProjection proj = reportReadRepository.findOverview(dtFrom, dtTo)
                .orElse(null);

        BigDecimal totalRevenue       = getOrZero(proj == null ? null : proj.getTotalRevenue());
        BigDecimal ticketRevenue      = getOrZero(proj == null ? null : proj.getTicketRevenue());
        BigDecimal foodRevenue        = getOrZero(proj == null ? null : proj.getFoodRevenue());
        Long       totalTicketsSold   = proj == null ? 0L : nullToZeroLong(proj.getTotalTicketsSold());
        Long       totalBookingsPaid  = proj == null ? 0L : nullToZeroLong(proj.getTotalBookingsPaid());
        BigDecimal totalDiscount      = getOrZero(proj == null ? null : proj.getTotalDiscount());
        BigDecimal aov                = calcAov(totalRevenue, totalBookingsPaid);

        return BusinessOverviewReportDTO.builder()
                .from(from)
                .to(to)
                .totalRevenue(totalRevenue)
                .ticketRevenue(ticketRevenue)
                .foodRevenue(foodRevenue)
                .totalTicketsSold(totalTicketsSold)
                .totalBookingsPaid(totalBookingsPaid)
                .totalDiscount(totalDiscount)
                .averageOrderValue(aov)
                .build();
    }

    public List<DailyRevenueReportDTO> getDailyRevenue(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.plusDays(1).atStartOfDay();

        List<DailyRevenueProjection> rows = reportReadRepository.findDailyRevenue(dtFrom, dtTo);

        return rows.stream()
                .map(r -> DailyRevenueReportDTO.builder()
                        .date(r.getReportDate())
                        .revenue(getOrZero(r.getRevenue()))
                        .bookingCount(nullToZeroLong(r.getBookingCount()))
                        .ticketCount(nullToZeroLong(r.getTicketCount()))
                        .build())
                .toList();
    }

    public List<MovieRevenueReportDTO> getRevenueByMovie(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.plusDays(1).atStartOfDay();

        List<MovieRevenueProjection> rows = reportReadRepository.findRevenueByMovie(dtFrom, dtTo);

        return rows.stream()
                .map(r -> MovieRevenueReportDTO.builder()
                        .movieId(r.getMovieId())
                        .movieTitle(r.getMovieTitle())
                        .ticketRevenue(getOrZero(r.getTicketRevenue()))
                        .ticketsSold(nullToZeroLong(r.getTicketsSold()))
                        .bookingCount(nullToZeroLong(r.getBookingCount()))
                        .build())
                .toList();
    }

    public List<CinemaRevenueReportDTO> getRevenueByCinema(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.plusDays(1).atStartOfDay();

        List<CinemaRevenueProjection> rows = reportReadRepository.findRevenueByCinema(dtFrom, dtTo);

        return rows.stream()
                .map(r -> CinemaRevenueReportDTO.builder()
                        .cinemaId(r.getCinemaId())
                        .cinemaName(r.getCinemaName())
                        .ticketRevenue(getOrZero(r.getTicketRevenue()))
                        .ticketsSold(nullToZeroLong(r.getTicketsSold()))
                        .bookingCount(nullToZeroLong(r.getBookingCount()))
                        .build())
                .toList();
    }


    /**
     * Validate: from không null, to không null, from <= to, span <= MAX_DAYS.
     */
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

    /**
     * AOV = totalRevenue / totalBookingsPaid.
     * Trả về 0 nếu không có booking nào.
     */
    private BigDecimal calcAov(BigDecimal totalRevenue, Long totalBookingsPaid) {
        if (totalBookingsPaid == null || totalBookingsPaid == 0L) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(BigDecimal.valueOf(totalBookingsPaid), 2, RoundingMode.HALF_UP);
    }
}
