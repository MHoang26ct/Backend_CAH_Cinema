package com.uit.backend_cinema.modules.price_config.api.dto.response;

import java.math.BigDecimal;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import lombok.Data;

@Data
public class PriceConfigResponse {
    private Long configId;
    private DayType dayType;
    private TimeSlot timeSlot;
    private MovieFormat movieFormat;
    private BigDecimal multiplier;
}
