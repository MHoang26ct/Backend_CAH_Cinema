package com.uit.backend_cinema.modules.seat.api.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SeatTypeDTO {
    private Long seatTypeId;
    private String typeName;       
    private BigDecimal priceMultiplier;
}
