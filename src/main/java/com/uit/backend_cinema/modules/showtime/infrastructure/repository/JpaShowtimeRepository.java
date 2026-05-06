package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface JpaShowtimeRepository extends JpaRepository<ShowtimeJpaEntity, Long> {

    @Query("""
        SELECT s FROM ShowtimeJpaEntity s
        WHERE s.movieId = :movieId
        AND s.roomId = :roomId
        AND s.startTime >= :startOfDay
        AND s.startTime < :endOfDay
        ORDER BY s.startTime ASC
    """)
    List<ShowtimeJpaEntity> findAllByMovieIdAndDate(@Param("movieId") Long movieId,
                                                    @Param("roomId") Long roomId,
                                                    @Param("startOfDay") LocalDateTime startOfDay,
                                                    @Param("endOfDay") LocalDateTime endOfDay);
}
