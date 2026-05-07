package com.uit.backend_cinema.modules.movies.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.infrastructure.entity.MovieJpaEntity;

@Mapper(componentModel = "spring", uses = {GenreInfraMapper.class})
public interface  MovieInfraMapper {
    Movie toDomain(MovieJpaEntity entity);

    @Mapping(target = "genres", ignore = true)
    MovieJpaEntity toInfrastructure(Movie movie);
}
