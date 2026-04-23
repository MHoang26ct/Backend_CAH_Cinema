package com.uit.backend_cinema.modules.user.movies.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.user.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.user.movies.domain.repository.MovieRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
