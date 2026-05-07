package com.uit.backend_cinema.modules.movies.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.movies.infrastructure.entity.GenreJpaEntity;

@Mapper(componentModel = "spring")
public interface GenreInfraMapper {
    GenreJpaEntity toInfrastructure(Genre genre);

    Genre toDomain(GenreJpaEntity genreJpaEntity);
}
