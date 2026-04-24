package com.uit.backend_cinema.modules.movies.infrastructure.mapper;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.movies.infrastructure.entity.GenreJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreInfraMapper {
    GenreJpaEntity toInfrastructure(Genre genre);

    Genre toDomain(GenreJpaEntity genreJpaEntity);
}
