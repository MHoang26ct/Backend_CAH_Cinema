package com.uit.backend_cinema.modules.food_order.api.entity;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class FoodOrderResponseDTO {
    private BigDecimal totalPrice;
    private List<FoodOrderItemResponseDTO> items;
}
