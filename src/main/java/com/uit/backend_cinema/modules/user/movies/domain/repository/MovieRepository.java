package com.uit.backend_cinema.modules.user.movies.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uit.backend_cinema.modules.user.movies.domain.entity.Movie;

import java.util.Optional;

public interface MovieRepository {
    Optional<Movie> findById(Long movieId);
    Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable);
}
