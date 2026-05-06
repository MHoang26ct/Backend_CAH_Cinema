package com.uit.backend_cinema.modules.showtime.domain.repository;

import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRepository {
    Optional<Showtime> findById(Long showtimeId);
    MovieShowtimes findShowtimesByMovieId(Long movieId, LocalDate date);
    void save(Showtime showtime);
    List<Showtime> findAllByMovieIdAndRoomIdAndDate(Long movieId, Long roomId, LocalDate date);
    List<CinemaShowtimes> findShowtimesByCinemaId(Long cinemaId, LocalDate date);
}
