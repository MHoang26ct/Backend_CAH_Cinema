package com.uit.backend_cinema.modules.showtime.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeSummaryDTO {
    private Long showtimeId;
    private String format;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private String status;
    private String roomName;
    private String cinemaName;
}
