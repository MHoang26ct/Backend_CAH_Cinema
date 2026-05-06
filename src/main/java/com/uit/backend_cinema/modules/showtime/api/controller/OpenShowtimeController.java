package com.uit.backend_cinema.modules.showtime.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.showtime.api.dto.CinemaShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.MovieShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeDetailDTO;
import com.uit.backend_cinema.modules.showtime.api.mapper.ShowtimeApiMapper;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/showtimes")
public class OpenShowtimeController {
    private final ShowtimeService showtimeService;
    private final ShowtimeApiMapper mapper;

    public OpenShowtimeController(ShowtimeService showtimeService, ShowtimeApiMapper mapper) {
        this.showtimeService = showtimeService;
        this.mapper = mapper;
    }

    // GET /api/v1/public/showtimes/movies/1?date=2025-05-10
    @GetMapping("/movies/{movieId}")
    public ResponseEntity<?> getShowtimesByMovie(
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        MovieShowtimesResponseDTO result = mapper.toMovieShowtimesResponseDto(
                showtimeService.getShowtimesByMovieId(movieId, date)
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // GET /api/v1/public/showtimes/cinemas/1?date=2025-05-10
    @GetMapping("/cinemas/{cinemaId}")
    public ResponseEntity<?> getShowtimesByCinema(
            @PathVariable Long cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<CinemaShowtimesResponseDTO> result = showtimeService.getShowtimesByCinemaId(cinemaId, date).stream()
                .map(mapper::toCinemaShowtimesResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
