package com.uit.backend_cinema.modules.movies.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.repository.MovieRepository;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;

@Service
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable) {
        Sort sort = pageable.getSort();
        List<Sort.Order> orders = new java.util.ArrayList<>();
        for (Sort.Order order : sort) {
            if ("releaseDate".equals(order.getProperty())) {
                orders.add(new Sort.Order(order.getDirection(), "release_date"));
            } else if ("createdAt".equals(order.getProperty())) {
                orders.add(new Sort.Order(order.getDirection(), "created_at"));
            } else {
                orders.add(order);
            }
        }
        Pageable nativePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
        return movieRepository.search(title, genreId, ageRating, nativePageable);
    }

    public FeaturedMoviesResult getFeaturedMovies() {
        List<Movie> nowShowing = movieRepository.findNowShowing();
        List<Movie> upcoming = movieRepository.findUpcoming();
        return new FeaturedMoviesResult(nowShowing, upcoming);
    }

    public record FeaturedMoviesResult(List<Movie> nowShowing, List<Movie> upcoming) {}

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
        showtimeRepository.softDeleteByMovieId(id);
        movie.setIsDeleted(true);
        return movieRepository.save(movie);
    }
}
