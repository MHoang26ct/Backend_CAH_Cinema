package com.uit.backend_cinema.modules.comment.domain.repository;

import java.util.Optional;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;

public interface MovieCommentRepository {
    Optional<MovieComment> findById(Long commentId);
    MovieComment save(MovieComment comment);
    Slice<MovieComment> findByMovieId(Long movieId, Pageable pageable);
    long countCommentsByUserAndMovie(Long userId, Long movieId);
    long countCheckedInBookingsByUserAndMovie(Long userId, Long movieId);
}
