package com.uit.backend_cinema.modules.price_config.api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class UpdateHolidayRequest {
    @NotNull(message = "Id không được trống")
    private Long holidayId;

    @NotNull(message = "Ngày không được trống")
    private LocalDate date;

    @NotBlank(message = "Tên không được trống")
    private String name;

    @NotNull(message = "Loại không được trống")
    private Boolean isRecurring;
}
