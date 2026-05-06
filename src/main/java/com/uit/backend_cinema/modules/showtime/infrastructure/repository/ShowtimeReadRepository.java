package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeReadRepository {
    List<MovieShowtimeRowDto> findMovieShowtimeRowsByDate(Long movieId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    List<CinemaShowtimeRowDto> findCinemaShowtimeRowsByDate(Long cinemaId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
