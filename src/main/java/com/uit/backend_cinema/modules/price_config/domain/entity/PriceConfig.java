package com.uit.backend_cinema.modules.price_config.domain.entity;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceConfig {
    private Long configId;
    private MovieFormat movieFormat;
    private DayType dayType;
    private TimeSlot timeSlot;
    private BigDecimal multiplier;
}
