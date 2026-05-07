package com.uit.backend_cinema.modules.showtime.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import lombok.Data;

@Data
public class CreateShowtimeDTO {
    @NotNull(message = "ID phim không được trống")
    @Min(value = 1, message = "ID phim phải là số dương")
    private Long movieId;

    @NotNull(message = "ID phòng không được trống")
    @Min(value = 1, message = "ID phòng phải là số dương")
    private Long roomId;

    @NotNull(message = "Format không được trống")
    private MovieFormat format;

    @NotNull(message = "Giờ bắt đầu không được trống")
    private LocalDateTime startTime;

    @NotNull(message = "Giờ kết thúc không được trống")
    private LocalDateTime endTime;

    @NotNull(message = "Giá gốc không được trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá gốc phải là số dương")
    private BigDecimal basePrice;
}
