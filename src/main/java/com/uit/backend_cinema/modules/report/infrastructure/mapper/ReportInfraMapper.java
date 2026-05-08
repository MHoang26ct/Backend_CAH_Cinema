package com.uit.backend_cinema.modules.report.infrastructure.mapper;

import com.uit.backend_cinema.modules.report.domain.model.BusinessOverview;
import com.uit.backend_cinema.modules.report.domain.model.CinemaRevenue;
import com.uit.backend_cinema.modules.report.domain.model.DailyRevenue;
import com.uit.backend_cinema.modules.report.domain.model.MovieRevenue;
import com.uit.backend_cinema.modules.report.infrastructure.dto.CinemaRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.DailyRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.MovieRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.OverviewProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportInfraMapper {

    @Mapping(target = "from", ignore = true)
    @Mapping(target = "to", ignore = true)
    @Mapping(target = "averageOrderValue", ignore = true)
    BusinessOverview toDomain(OverviewProjection projection);

    DailyRevenue toDomain(DailyRevenueProjection projection);

    MovieRevenue toDomain(MovieRevenueProjection projection);

    CinemaRevenue toDomain(CinemaRevenueProjection projection);
}
