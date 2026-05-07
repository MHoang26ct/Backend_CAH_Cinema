package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;

public interface JpaShowtimeRepository extends JpaRepository<ShowtimeJpaEntity, Long> {

    @Query("""
        SELECT s FROM ShowtimeJpaEntity s
        WHERE s.roomId = :roomId
        AND s.startTime >= :startOfDay
        AND s.startTime < :endOfDay
        ORDER BY s.startTime ASC
    """)
    List<ShowtimeJpaEntity> findAllByRoomIdAndDate(@Param("roomId") Long roomId,
                                                   @Param("startOfDay") LocalDateTime startOfDay,
                                                   @Param("endOfDay") LocalDateTime endOfDay);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShowtimeJpaEntity s set s.isDeleted = true where s.roomId = :roomId and s.isDeleted = false")
    void softDeleteByRoomId(@Param("roomId") Long roomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShowtimeJpaEntity s set s.isDeleted = true where s.roomId in :roomIds and s.isDeleted = false")
    void softDeleteByRoomIds(@Param("roomIds") List<Long> roomIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShowtimeJpaEntity s set s.isDeleted = true where s.movieId = :movieId and s.isDeleted = false")
    void softDeleteByMovieId(@Param("movieId") Long movieId);
}
