package com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaShowtimeRowDto {
    private Long showtimeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String format;
    private BigDecimal basePrice;
    private String status;
    private String roomName;
    private Long movieId;
    private String title;
    private String posterUrl;
    private String ageRating;
}
