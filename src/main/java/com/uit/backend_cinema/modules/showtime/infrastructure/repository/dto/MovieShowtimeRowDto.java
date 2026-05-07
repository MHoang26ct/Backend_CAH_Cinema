package com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieShowtimeRowDto {
    private Long showtimeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String format;
    private BigDecimal basePrice;
    private String status;
    private String roomName;
    private Long cinemaId;
    private String cinemaName;
    private String address;
    private Long movieId;
    private String title;
    private String description;
}
