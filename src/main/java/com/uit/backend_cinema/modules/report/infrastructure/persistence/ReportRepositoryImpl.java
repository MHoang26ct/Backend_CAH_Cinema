package com.uit.backend_cinema.modules.report.infrastructure.persistence;

import com.uit.backend_cinema.modules.report.domain.model.BusinessOverview;
import com.uit.backend_cinema.modules.report.domain.model.CinemaRevenue;
import com.uit.backend_cinema.modules.report.domain.model.DailyRevenue;
import com.uit.backend_cinema.modules.report.domain.model.MovieRevenue;
import com.uit.backend_cinema.modules.report.domain.repository.ReportRepository;
import com.uit.backend_cinema.modules.report.infrastructure.mapper.ReportInfraMapper;
import com.uit.backend_cinema.modules.report.infrastructure.repository.ReportReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {

    private final ReportReadRepository reportReadRepository;
    private final ReportInfraMapper reportInfraMapper;

    @Override
    public Optional<BusinessOverview> findOverview(LocalDateTime from, LocalDateTime to) {
        return reportReadRepository.findOverview(from, to)
                .map(reportInfraMapper::toDomain);
    }

    @Override
    public List<DailyRevenue> findDailyRevenue(LocalDateTime from, LocalDateTime to) {
        return reportReadRepository.findDailyRevenue(from, to).stream()
                .map(reportInfraMapper::toDomain)
                .toList();
    }

    @Override
    public List<MovieRevenue> findRevenueByMovie(LocalDateTime from, LocalDateTime to) {
        return reportReadRepository.findRevenueByMovie(from, to).stream()
                .map(reportInfraMapper::toDomain)
                .toList();
    }

    @Override
    public List<CinemaRevenue> findRevenueByCinema(LocalDateTime from, LocalDateTime to) {
        return reportReadRepository.findRevenueByCinema(from, to).stream()
                .map(reportInfraMapper::toDomain)
                .toList();
    }
}
