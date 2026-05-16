package com.uit.backend_cinema.modules.movies.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.uit.backend_cinema.modules.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.movies.api.dto.MovieSummaryDTO;
import com.uit.backend_cinema.modules.movies.api.dto.UpdateOrCreateMovieDTO;
import com.uit.backend_cinema.modules.movies.domain.entity.AgeRating;
import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;

@Mapper(componentModel = "spring", uses = {GenreApiMapper.class})
public interface  MovieApiMapper {
    MovieSummaryDTO toSummaryDto(Movie movie);
    MovieDetailDTO toDetailDto(Movie movie);

    @Named("mapGenreIdToGenre")
    default Genre mapGenreIdToGenre(Long id) {
        if (id == null) return null;
        Genre genre = new Genre();
        genre.setGenreId(id);
        return genre;
    }

    @Named("mapStringToAgeRating")
    default AgeRating mapStringToAgeRating(String value) {
        if (value == null || value.isBlank()) return null;
        return AgeRating.valueOf(value.toUpperCase());
    }

    @Mapping(target = "movieId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(source = "genreIdList", target = "genres", qualifiedByName = "mapGenreIdToGenre")
    @Mapping(source = "ageRating", target = "ageRating", qualifiedByName = "mapStringToAgeRating")
    Movie toDomain(UpdateOrCreateMovieDTO requestDTO);
}
