package com.uit.backend_cinema.modules.showtime.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

public interface ShowtimeReadRepository {
    List<MovieShowtimeRowDto> findMovieShowtimeRowsByDate(Long movieId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    List<CinemaShowtimeRowDto> findCinemaShowtimeRowsByDate(Long cinemaId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
