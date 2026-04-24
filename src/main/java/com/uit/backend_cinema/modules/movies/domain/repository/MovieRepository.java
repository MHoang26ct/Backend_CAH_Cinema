package com.uit.backend_cinema.modules.movies.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uit.backend_cinema.modules.movies.domain.entity.Movie;

import java.time.LocalDate;
import java.util.Optional;

public interface MovieRepository {
    Optional<Movie> findById(Long movieId);
    boolean isDuplicate(String title, LocalDate releaseDate);
    Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable);
    Movie save(Movie movie);
}
