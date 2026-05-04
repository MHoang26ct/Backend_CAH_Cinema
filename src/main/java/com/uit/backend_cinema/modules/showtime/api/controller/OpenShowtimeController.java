package com.uit.backend_cinema.modules.showtime.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeDetailDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeSummaryDTO;
import com.uit.backend_cinema.modules.showtime.api.mapper.ShowtimeApiMapper;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/public/showtimes")
public class OpenShowtimeController {
    private final ShowtimeService showtimeService;
    private final ShowtimeApiMapper mapper;

    public OpenShowtimeController(ShowtimeService showtimeService, ShowtimeApiMapper mapper) {
        this.showtimeService = showtimeService;
        this.mapper = mapper;
    }

    // GET /api/v1/public/showtimes?movieId=1&date=2025-05-10
    // GET /api/v1/public/showtimes?cinemaId=1&date=2025-05-10
    @GetMapping
    public ResponseEntity<?> getShowtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ShowtimeSummaryDTO> result;

        if (movieId != null) {
            result = showtimeService.findByMovieAndDate(movieId, date, pageable)
                    .map(mapper::toSummaryDto);
        } else if (cinemaId != null) {
            result = showtimeService.findByCinemaAndDate(cinemaId, date, pageable)
                    .map(mapper::toSummaryDto);
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Cần truyền movieId hoặc cinemaId"));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // GET /api/v1/public/showtimes/1
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        ShowtimeDetailDTO result = mapper.toDetailDto(showtimeService.getById(id));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
