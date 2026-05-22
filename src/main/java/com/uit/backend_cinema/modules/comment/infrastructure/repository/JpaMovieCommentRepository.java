package com.uit.backend_cinema.modules.comment.infrastructure.repository;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.comment.infrastructure.entity.MovieCommentJpaEntity;

@Repository
public interface JpaMovieCommentRepository extends JpaRepository<MovieCommentJpaEntity, Long> {

    @Query("SELECT mc FROM MovieCommentJpaEntity mc JOIN FETCH mc.user WHERE mc.movieId = :movieId")
    Slice<MovieCommentJpaEntity> findByMovieId(@Param("movieId") Long movieId, Pageable pageable);

    long countByUserIdAndMovieId(Long userId, Long movieId);

    @Query(value = "SELECT COUNT(DISTINCT b.booking_id) " +
                   "FROM bookings b " +
                   "JOIN showtimes s ON b.showtime_id = s.showtime_id " +
                   "WHERE b.user_id = :userId " +
                   "  AND s.movie_id = :movieId " +
                   "  AND b.status = 'CHECKED_IN' " +
                   "  AND b.is_deleted = false", nativeQuery = true)
    long countCheckedInBookings(@Param("userId") Long userId, @Param("movieId") Long movieId);
}
