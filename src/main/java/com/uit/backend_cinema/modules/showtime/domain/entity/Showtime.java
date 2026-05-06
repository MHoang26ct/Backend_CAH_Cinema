package com.uit.backend_cinema.modules.showtime.domain.entity;

import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Showtime {
    private Long showtimeId;
    private Long roomId;
    private Long movieId;
    private MovieFormat format;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Boolean isDeleted;
}
