package com.uit.backend_cinema.modules.movies.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.repository.MovieRepository;
import com.uit.backend_cinema.modules.movies.infrastructure.entity.GenreJpaEntity;
import com.uit.backend_cinema.modules.movies.infrastructure.entity.MovieJpaEntity;
import com.uit.backend_cinema.modules.movies.infrastructure.mapper.MovieInfraMapper;
import com.uit.backend_cinema.modules.movies.infrastructure.repository.JpaMovieRepository;

@Repository
public class MovieRepositoryImpl implements MovieRepository{
    private final JpaMovieRepository jpaMovieRepository;
    private final MovieInfraMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public MovieRepositoryImpl(JpaMovieRepository jpaMovieRepository, MovieInfraMapper mapper) {
        this.jpaMovieRepository = jpaMovieRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean isDuplicate(String title, LocalDate releaseDate) {
        return jpaMovieRepository.existsByTitleAndReleaseDateAfter(title, releaseDate);
    }

    @Override
    public Optional<Movie> findById(Long movieId) {
        return jpaMovieRepository.findById(movieId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Movie> search(String title, Long genreId, String ageRating, Pageable pageable) {
        return jpaMovieRepository.searchMovies(title, genreId, ageRating, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Movie save(Movie movie) {
        MovieJpaEntity jpaEntity = mapper.toInfrastructure(movie);
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            jpaEntity.setGenres(
                    movie.getGenres().stream()
                            .map(genre -> entityManager.getReference(GenreJpaEntity.class, genre.getGenreId()))
                            .collect(Collectors.toSet())
            );
        }
        return mapper.toDomain(jpaMovieRepository.save(jpaEntity));
    }

    @Override
    public List<Movie> findNowShowing() {
        return jpaMovieRepository.findNowShowing().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findUpcoming() {
        return jpaMovieRepository.findUpcoming().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
