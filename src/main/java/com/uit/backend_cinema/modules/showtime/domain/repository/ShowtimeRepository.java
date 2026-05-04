package com.uit.backend_cinema.modules.showtime.domain.repository;

import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface  ShowtimeRepository {
    Optional<Showtime> findById(Long showtimeId);
    Page<Showtime> findByMovieAndDate(Long movieId, LocalDate date, Pageable pageable);
    Page<Showtime> findByCinemaAndDate(Long cinemaId, LocalDate date, Pageable pageable);
}
