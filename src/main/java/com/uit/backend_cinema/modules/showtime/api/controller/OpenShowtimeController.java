package com.uit.backend_cinema.modules.showtime.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.showtime.api.dto.CinemaShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.MovieShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.mapper.ShowtimeApiMapper;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;

@RestController
@RequestMapping("/api/v1/public/showtimes")
public class OpenShowtimeController {
    private static final int MAX_ADVANCE_DAYS = 7;

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
        validateDate(date);
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
        validateDate(date);
        List<CinemaShowtimesResponseDTO> result = showtimeService.getShowtimesByCinemaId(cinemaId, date).stream()
                .map(mapper::toCinemaShowtimesResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void validateDate(LocalDate date) {
        if (date.isAfter(LocalDate.now().plusDays(MAX_ADVANCE_DAYS))) {
            throw new BusinessException(
                    "Chỉ có thể xem lịch chiếu trong vòng 7 ngày tới",
                    ErrorCode.VALIDATION_FAILED);
        }
    }
}
