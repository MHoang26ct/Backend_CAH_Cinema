package com.uit.backend_cinema.modules.showtime.infrastructure.mapper;

import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface  ShowtimeInfraMapper {
    @Mapping(target = "roomName", ignore = true)
    @Mapping(target = "cinemaId", ignore = true)
    @Mapping(target = "cinemaName", ignore = true)
    @Mapping(target = "movieTitle", ignore = true)
    @Mapping(target = "moviePosterUrl", ignore = true)
    @Mapping(target = "movieDuration", ignore = true)
    Showtime toDomain(ShowtimeJpaEntity entity);
}
