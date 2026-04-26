package com.uit.backend_cinema.modules.price_config.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class Holiday {
    private Long holidayId;
    private String name;
    private LocalDate date;
    private Boolean isRecurring;
}
