package com.uit.backend_cinema.modules.food_order.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodOrder {
    private long foodOrderId;
    private long bookingId;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;

    private List<FoodOrderItem> items;
}
