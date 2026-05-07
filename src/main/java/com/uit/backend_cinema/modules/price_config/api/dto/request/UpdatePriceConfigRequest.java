package com.uit.backend_cinema.modules.price_config.api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import lombok.Data;

@Data
public class UpdatePriceConfigRequest {
    @NotNull(message = "Id không được trống")
    private Long configId;

    private DayType dayType;
    private TimeSlot timeSlot;
    private MovieFormat movieFormat;

    @NotNull(message = "Hệ số giá không được trống")
    @Positive(message = "Hệ số giá phải là số dương")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal multiplier;
}
