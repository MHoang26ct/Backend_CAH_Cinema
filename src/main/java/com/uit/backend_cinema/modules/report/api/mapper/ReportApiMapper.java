package com.uit.backend_cinema.modules.report.api.mapper;

import com.uit.backend_cinema.modules.report.api.dto.BusinessOverviewReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.CinemaRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.DailyRevenueReportDTO;
import com.uit.backend_cinema.modules.report.api.dto.MovieRevenueReportDTO;
import com.uit.backend_cinema.modules.report.domain.model.BusinessOverview;
import com.uit.backend_cinema.modules.report.domain.model.CinemaRevenue;
import com.uit.backend_cinema.modules.report.domain.model.DailyRevenue;
import com.uit.backend_cinema.modules.report.domain.model.MovieRevenue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportApiMapper {

    BusinessOverviewReportDTO toDTO(BusinessOverview model);

    @Mapping(source = "reportDate", target = "date")
    DailyRevenueReportDTO toDTO(DailyRevenue model);

    MovieRevenueReportDTO toDTO(MovieRevenue model);

    CinemaRevenueReportDTO toDTO(CinemaRevenue model);
}
