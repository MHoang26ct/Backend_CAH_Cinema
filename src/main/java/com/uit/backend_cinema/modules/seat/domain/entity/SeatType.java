package com.uit.backend_cinema.modules.seat.domain.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class SeatType {
    private Long seatTypeId;
    private String typeName;
    private BigDecimal priceMultiplier;
}
