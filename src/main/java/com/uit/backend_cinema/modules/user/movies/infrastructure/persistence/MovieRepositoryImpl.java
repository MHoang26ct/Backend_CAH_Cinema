package com.uit.backend_cinema.modules.user.movies.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.user.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.user.movies.domain.repository.MovieRepository;
import com.uit.backend_cinema.modules.user.movies.infrastructure.mapper.MovieInfraMapper;
import com.uit.backend_cinema.modules.user.movies.infrastructure.repository.JpaMovieRepository;

import java.util.Optional;

@Repository
public class MovieRepositoryImpl implements MovieRepository{
    private final JpaMovieRepository jpaMovieRepository;
    private final MovieInfraMapper mapper;

    public MovieRepositoryImpl(JpaMovieRepository jpaMovieRepository, MovieInfraMapper mapper) {
        this.jpaMovieRepository = jpaMovieRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Movie> findById(Long movieId) {
        return jpaMovieRepository.findById(movieId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(mapper::toDomain);
    }

    @Override
    public Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable) {
        return jpaMovieRepository.searchMovies(title, genreId, ageRating, pageable)
                .map(mapper::toDomain);
    }
}
