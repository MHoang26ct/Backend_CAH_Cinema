package com.uit.backend_cinema.modules.seat.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeatTypeDTO {
    private Long seatTypeId;
    private String typeName;       
    private BigDecimal priceMultiplier;
}
