package com.uit.backend_cinema.modules.food_order.domain.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodOrderItem {
    private Long foodId;
    private String foodName;
    private int quantity;
    private BigDecimal price;
}
