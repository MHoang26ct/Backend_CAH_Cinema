package com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
