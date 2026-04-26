package com.uit.backend_cinema.modules.price_config.api.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidaysResponse {
    private Long holidayId;
    private LocalDate date;
    private String name;
    private Boolean isRecurring;
}
