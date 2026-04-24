package com.uit.backend_cinema.modules.movies.domain.repository;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;

import java.util.List;

public interface GenreRepository {
    List<Genre> findAll();
    List<Genre> findAllById(List<Long> genreIdList);
}
