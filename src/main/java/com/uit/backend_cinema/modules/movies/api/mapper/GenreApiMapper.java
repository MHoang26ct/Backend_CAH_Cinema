package com.uit.backend_cinema.modules.movies.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.movies.api.dto.GenreDTO;
import com.uit.backend_cinema.modules.movies.domain.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreApiMapper {

    GenreDTO toDTO(Genre genre);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Genre toDomain(GenreDTO genreDTO);
}
