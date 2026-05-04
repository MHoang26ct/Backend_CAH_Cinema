package com.uit.backend_cinema.modules.showtime.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeDetailDTO {
    private Long showtimeId;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private Integer movieDuration;
    private Long roomId;
    private String roomName;
    private Long cinemaId;
    private String cinemaName;
    private String format;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private String status;
}