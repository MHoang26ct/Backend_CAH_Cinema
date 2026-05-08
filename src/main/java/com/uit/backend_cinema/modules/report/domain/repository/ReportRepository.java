package com.uit.backend_cinema.modules.report.domain.repository;

import com.uit.backend_cinema.modules.report.domain.model.BusinessOverview;
import com.uit.backend_cinema.modules.report.domain.model.CinemaRevenue;
import com.uit.backend_cinema.modules.report.domain.model.DailyRevenue;
import com.uit.backend_cinema.modules.report.domain.model.MovieRevenue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    Optional<BusinessOverview> findOverview(LocalDateTime from, LocalDateTime to);

    List<DailyRevenue> findDailyRevenue(LocalDateTime from, LocalDateTime to);

    List<MovieRevenue> findRevenueByMovie(LocalDateTime from, LocalDateTime to);

    List<CinemaRevenue> findRevenueByCinema(LocalDateTime from, LocalDateTime to);
}
