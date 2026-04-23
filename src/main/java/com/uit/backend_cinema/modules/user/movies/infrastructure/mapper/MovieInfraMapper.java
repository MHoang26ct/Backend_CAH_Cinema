package com.uit.backend_cinema.modules.user.movies.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.user.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.user.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.user.movies.infrastructure.entity.GenreJpaEntity;
import com.uit.backend_cinema.modules.user.movies.infrastructure.entity.MovieJpaEntity;

@Mapper(componentModel = "spring")
public interface  MovieInfraMapper {
    Movie toDomain(MovieJpaEntity entity);
    Genre toDomain(GenreJpaEntity entity);
}
