package com.uit.backend_cinema.modules.showtime.domain.entity;

import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class Showtime {
    private Long showtimeId;
    private Long roomId;
    private String roomName;
    private Long cinemaId;
    private String cinemaName;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private Integer movieDuration;
    private MovieFormat format;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
}
