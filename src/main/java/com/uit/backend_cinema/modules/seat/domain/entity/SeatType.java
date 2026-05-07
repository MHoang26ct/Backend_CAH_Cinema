package com.uit.backend_cinema.modules.seat.domain.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatType {
    private Long seatTypeId;
    private String typeName;
    private BigDecimal priceMultiplier;
}
