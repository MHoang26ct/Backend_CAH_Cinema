package com.uit.backend_cinema.modules.price_config.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHolidayRequest {
    @NotNull(message = "Ngày không được trống")
    private LocalDate date;

    @NotBlank(message = "Tên không được trống")
    private String name;

    @NotNull(message = "Loại không được trống")
    private Boolean isRecurring;
}
