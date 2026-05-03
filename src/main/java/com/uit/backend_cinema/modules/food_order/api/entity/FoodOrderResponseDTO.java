package com.uit.backend_cinema.modules.food_order.api.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FoodOrderResponseDTO {
    private BigDecimal totalPrice;
    private List<FoodOrderItemResponseDTO> items;
}
