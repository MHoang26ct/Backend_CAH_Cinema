package com.uit.backend_cinema.modules.movies.domain.repository;

import java.util.List;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;

public interface GenreRepository {
    List<Genre> findAll();
    List<Genre> findAllById(List<Long> genreIdList);
}
