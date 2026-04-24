package com.uit.backend_cinema.modules.movies.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.repository.MovieRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MovieService {
     private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable) {
        return movieRepository.search(title, genreId, ageRating, pageable);
    }

    public Movie getById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phim", ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Movie createMovie(Movie movie) {
        boolean isDuplicate = movieRepository.isDuplicate(movie.getTitle(), movie.getReleaseDate());
        if (isDuplicate) {
            throw new BusinessException("Phim đã tồn tại", ErrorCode.DUPLICATE_RESOURCE);
        }
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie updateMovie(Long id, Movie movie) {
        Movie existingMovie = movieRepository.findById(id).orElse(null);
        if (existingMovie == null) {
            throw new BusinessException("Phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND);
        }
        existingMovie.setTitle(movie.getTitle());
        existingMovie.setDescription(movie.getDescription());
        existingMovie.setAgeRating(movie.getAgeRating());
        existingMovie.setDuration(movie.getDuration());
        existingMovie.setActorList(movie.getActorList());
        existingMovie.setDirectorName(movie.getDirectorName());
        existingMovie.setPosterUrl(movie.getPosterUrl());
        existingMovie.setTrailerUrl(movie.getTrailerUrl());
        existingMovie.setGenres(movie.getGenres());
        existingMovie.setReleaseDate(movie.getReleaseDate());
        return movieRepository.save(existingMovie);
    }

    @Transactional
    public Movie deleteMovie(Long id) {
        Optional<Movie> movieOpt = movieRepository.findById(id);
        if (movieOpt.isEmpty()) {
            throw new BusinessException("Phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND);
        }
        Movie movie = movieOpt.get();
        movie.setIsDeleted(true);
        return movieRepository.save(movie);
    }
}
