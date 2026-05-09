package com.uit.backend_cinema.modules.showtime.infrastructure.mapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import org.springframework.stereotype.Component;

import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

@Component
public class ShowtimeQueryMapper {

    public MovieShowtimeRowDto toMovieRowDto(Tuple row) {
        return new MovieShowtimeRowDto(
                getValue(row, "showtimeId", Long.class),
                toLocalDateTime(getValue(row, "startTime", Object.class)),
                toLocalDateTime(getValue(row, "endTime", Object.class)),
                toStringValue(getValue(row, "format", Object.class)),
                getValue(row, "basePrice", BigDecimal.class),
                toStringValue(getValue(row, "status", Object.class)),
                toStringValue(getValue(row, "roomName", Object.class)),
                getValue(row, "cinemaId", Long.class),
                toStringValue(getValue(row, "cinemaName", Object.class)),
                toStringValue(getValue(row, "address", Object.class)),
                getValue(row, "movieId", Long.class),
                toStringValue(getValue(row, "title", Object.class)),
                toStringValue(getValue(row, "description", Object.class))
        );
    }

    public CinemaShowtimeRowDto toCinemaRowDto(Tuple row) {
        return new CinemaShowtimeRowDto(
                getValue(row, "showtimeId", Long.class),
                toLocalDateTime(getValue(row, "startTime", Object.class)),
                toLocalDateTime(getValue(row, "endTime", Object.class)),
                toStringValue(getValue(row, "format", Object.class)),
                getValue(row, "basePrice", BigDecimal.class),
                toStringValue(getValue(row, "status", Object.class)),
                toStringValue(getValue(row, "roomName", Object.class)),
                getValue(row, "movieId", Long.class),
                toStringValue(getValue(row, "title", Object.class)),
                toStringValue(getValue(row, "posterUrl", Object.class)),
                toStringValue(getValue(row, "ageRating", Object.class))
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

    private <T> T getValue(Tuple row, String expectedAlias, Class<T> type) {
        for (TupleElement<?> element : row.getElements()) {
            String actualAlias = element.getAlias();
            if (isSameAlias(actualAlias, expectedAlias)) {
                Object value = row.get(element);
                return castValue(value, type);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T castValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type == Object.class) {
            return (T) value;
        }
        if (type == Long.class && value instanceof Number number) {
            return (T) Long.valueOf(number.longValue());
        }
        if (type == Integer.class && value instanceof Number number) {
            return (T) Integer.valueOf(number.intValue());
        }
        return type.cast(value);
    }

    private boolean isSameAlias(String actual, String expected) {
        if (Objects.equals(actual, expected)) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        String normalizedActual = normalizeAlias(actual);
        String normalizedExpected = normalizeAlias(expected);
        return normalizedActual.equals(normalizedExpected);
    }

    private String normalizeAlias(String alias) {
        return alias.replace("_", "").toLowerCase();
    }
}
