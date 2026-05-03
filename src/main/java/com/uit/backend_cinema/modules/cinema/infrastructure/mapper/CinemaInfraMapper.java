package com.uit.backend_cinema.modules.cinema.infrastructure.mapper;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.CinemaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CinemaInfraMapper {
    Cinema toDomain(CinemaJpaEntity entity);
    CinemaJpaEntity toEntity(Cinema domain);
}
