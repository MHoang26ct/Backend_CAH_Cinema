package com.uit.backend_cinema.modules.price_config.api.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class DeleteHolidayRequest {
    @NotNull(message = "holidayId is required")
    private Long holidayId;
}
