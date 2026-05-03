package com.uit.backend_cinema.modules.food_order.api.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodOrderItemResponseDTO {
    private String foodId;
    private String foodName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal total;
}
