package com.uit.backend_cinema.modules.price_config.domain.entity;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Holiday {
    private Long holidayId;
    private String name;
    private LocalDate date;
    private Boolean isRecurring;
}
