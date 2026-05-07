package com.uit.backend_cinema.modules.food_order.api.entity;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class FoodOrderItemResponseDTO {
    private String foodId;
    private String foodName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal total;
}
