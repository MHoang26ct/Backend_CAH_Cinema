package com.uit.backend_cinema.modules.showtime.infrastructure.mapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.persistence.Tuple;

import org.springframework.stereotype.Component;

import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

@Component
public class ShowtimeQueryMapper {

    public MovieShowtimeRowDto toMovieRowDto(Tuple row) {
        return new MovieShowtimeRowDto(
                row.get("showtimeId", Long.class),
                toLocalDateTime(row.get("startTime")),
                toLocalDateTime(row.get("endTime")),
                toStringValue(row.get("format")),
                row.get("basePrice", BigDecimal.class),
                toStringValue(row.get("status")),
                toStringValue(row.get("roomName")),
                row.get("cinemaId", Long.class),
                toStringValue(row.get("cinemaName")),
                toStringValue(row.get("address")),
                row.get("movieId", Long.class),
                toStringValue(row.get("title")),
                toStringValue(row.get("description"))
        );
    }

    public CinemaShowtimeRowDto toCinemaRowDto(Tuple row) {
        return new CinemaShowtimeRowDto(
                row.get("showtimeId", Long.class),
                toLocalDateTime(row.get("startTime")),
                toLocalDateTime(row.get("endTime")),
                toStringValue(row.get("format")),
                row.get("basePrice", BigDecimal.class),
                toStringValue(row.get("status")),
                toStringValue(row.get("roomName")),
                row.get("movieId", Long.class),
                toStringValue(row.get("title")),
                toStringValue(row.get("posterUrl")),
                toStringValue(row.get("ageRating"))
        );
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported datetime type: " + value.getClass().getName());
    }
}
