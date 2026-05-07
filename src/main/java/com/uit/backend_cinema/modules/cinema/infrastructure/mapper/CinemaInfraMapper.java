package com.uit.backend_cinema.modules.cinema.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.CinemaJpaEntity;

@Mapper(componentModel = "spring")
public interface CinemaInfraMapper {
    Cinema toDomain(CinemaJpaEntity entity);
    CinemaJpaEntity toEntity(Cinema domain);
}
