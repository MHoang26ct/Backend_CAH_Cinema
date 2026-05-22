package com.uit.backend_cinema.modules.comment.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.repository.MovieCommentRepository;
import com.uit.backend_cinema.modules.comment.infrastructure.entity.MovieCommentJpaEntity;
import com.uit.backend_cinema.modules.comment.infrastructure.mapper.MovieCommentInfraMapper;
import com.uit.backend_cinema.modules.comment.infrastructure.repository.JpaMovieCommentRepository;

@Repository
public class MovieCommentRepositoryImpl implements MovieCommentRepository {
    private final JpaMovieCommentRepository jpaMovieCommentRepository;
    private final MovieCommentInfraMapper mapper;

    public MovieCommentRepositoryImpl(JpaMovieCommentRepository jpaMovieCommentRepository, MovieCommentInfraMapper mapper) {
        this.jpaMovieCommentRepository = jpaMovieCommentRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MovieComment> findById(Long commentId) {
        return jpaMovieCommentRepository.findById(commentId)
                .map(mapper::toDomain);
    }

    @Override
    public MovieComment save(MovieComment comment) {
        MovieCommentJpaEntity entity = jpaMovieCommentRepository.save(mapper.toInfrastructure(comment));
        return mapper.toDomain(entity);
    }

    @Override
    public Slice<MovieComment> findByMovieId(Long movieId, Pageable pageable) {
        return jpaMovieCommentRepository.findByMovieId(movieId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public long countCommentsByUserAndMovie(Long userId, Long movieId) {
        return jpaMovieCommentRepository.countByUserIdAndMovieId(userId, movieId);
    }

    @Override
    public long countCheckedInBookingsByUserAndMovie(Long userId, Long movieId) {
        return jpaMovieCommentRepository.countCheckedInBookings(userId, movieId);
    }
}
