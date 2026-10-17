package com.uit.backend_cinema.modules.ticket.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInFoodItemDTO {
    private Long foodId;
    private String foodName;
    private Integer quantity;
    private BigDecimal price;
}
