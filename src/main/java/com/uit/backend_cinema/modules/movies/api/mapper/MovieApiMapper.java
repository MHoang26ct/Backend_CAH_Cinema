package com.uit.backend_cinema.modules.movies.api.mapper;

import com.uit.backend_cinema.modules.movies.api.dto.UpdateOrCreateMovieDTO;
import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.movies.api.dto.MovieSummaryDTO;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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

    @Mapping(target = "movieId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(source = "genreIdList", target = "genres", qualifiedByName = "mapGenreIdToGenre")
    Movie toDomain(UpdateOrCreateMovieDTO requestDTO);
}
