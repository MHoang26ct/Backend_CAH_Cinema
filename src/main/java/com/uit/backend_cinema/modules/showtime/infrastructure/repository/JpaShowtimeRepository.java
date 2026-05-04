package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JpaShowtimeRepository extends JpaRepository<ShowtimeJpaEntity, Long> {

    @Query("""
        SELECT s FROM ShowtimeJpaEntity s
        WHERE s.movieId = :movieId
        AND s.startTime >= :startOfDay
        AND s.startTime < :endOfDay
        AND s.status != 'HIDDEN'
        ORDER BY s.startTime ASC
    """)
    Page<ShowtimeJpaEntity> findByMovieAndDate(
        @Param("movieId") Long movieId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay,
        Pageable pageable
    );

    @Query(value = """
        SELECT s.* FROM showtimes s
        JOIN rooms r ON s.room_id = r.room_id
        WHERE r.cinema_id = :cinemaId
        AND s.start_time >= :startOfDay
        AND s.start_time < :endOfDay
        AND s.status != 'HIDDEN'
        ORDER BY s.start_time ASC
    """, nativeQuery = true)
    Page<ShowtimeJpaEntity> findByCinemaAndDate(
        @Param("cinemaId") Long cinemaId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay,
        Pageable pageable
    );
}
