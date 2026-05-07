package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.showtime.infrastructure.mapper.ShowtimeQueryMapper;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

@Repository
public class ShowtimeReadRepositoryImpl implements ShowtimeReadRepository {
    private final EntityManager entityManager;
    private final ShowtimeQueryMapper queryMapper;

    public ShowtimeReadRepositoryImpl(EntityManager entityManager, ShowtimeQueryMapper queryMapper) {
        this.entityManager = entityManager;
        this.queryMapper = queryMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MovieShowtimeRowDto> findMovieShowtimeRowsByDate(Long movieId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        String sql = """
            SELECT s.showtime_id as showtimeId, s.start_time as startTime, s.end_time as endTime, s.format as format,
                   s.base_price as basePrice, s.status as status, r.room_name as roomName,
                   c.cinema_id as cinemaId, c.name as cinemaName, c.address as address,
                   m.movie_id as movieId, m.title as title, m.description as description
            FROM showtimes s
            JOIN rooms r ON s.room_id = r.room_id
            JOIN cinemas c ON r.cinema_id = c.cinema_id
            JOIN movies m ON s.movie_id = m.movie_id
            WHERE s.movie_id = :movieId
              AND s.start_time >= :startOfDay
              AND s.start_time < :endOfDay
              AND s.status = 'AVAILABLE'
              AND r.is_deleted = false
              AND c.is_deleted = false
              AND m.is_deleted = false
            ORDER BY c.cinema_id, s.start_time
        """;

        List<Tuple> rows = entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("movieId", movieId)
                .setParameter("startOfDay", startOfDay)
                .setParameter("endOfDay", endOfDay)
                .getResultList();

        return rows.stream().map(queryMapper::toMovieRowDto).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CinemaShowtimeRowDto> findCinemaShowtimeRowsByDate(Long cinemaId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        String sql = """
            SELECT s.showtime_id as showtimeId, s.start_time as startTime, s.end_time as endTime, s.format as format,
                   s.base_price as basePrice, s.status as status, r.room_name as roomName,
                   m.movie_id as movieId, m.title as title, m.poster_url as posterUrl, m.age_rating as ageRating
            FROM showtimes s
            JOIN rooms r ON s.room_id = r.room_id
            JOIN movies m ON s.movie_id = m.movie_id
            WHERE r.cinema_id = :cinemaId
              AND s.start_time >= :startOfDay
              AND s.start_time < :endOfDay
              AND s.status = 'AVAILABLE'
              AND r.is_deleted = false
              AND m.is_deleted = false
            ORDER BY m.movie_id, s.start_time
        """;

        List<Tuple> rows = entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("cinemaId", cinemaId)
                .setParameter("startOfDay", startOfDay)
                .setParameter("endOfDay", endOfDay)
                .getResultList();

        return rows.stream().map(queryMapper::toCinemaRowDto).toList();
    }
}
