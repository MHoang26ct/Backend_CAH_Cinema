package com.uit.backend_cinema.modules.user.movies.api.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.user.movies.api.dto.GenreDTO;
import com.uit.backend_cinema.modules.user.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.user.movies.api.dto.MovieSummaryDTO;
import com.uit.backend_cinema.modules.user.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.user.movies.domain.entity.Movie;

@Mapper(componentModel = "spring")
public interface  MovieApiMapper {
    MovieSummaryDTO toSummaryDto(Movie movie);
    MovieDetailDTO toDetailDto(Movie movie);
    GenreDTO toDto(Genre genre);
}
