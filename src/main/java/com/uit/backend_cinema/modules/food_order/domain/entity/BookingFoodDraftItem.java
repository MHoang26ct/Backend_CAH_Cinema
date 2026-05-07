package com.uit.backend_cinema.modules.food_order.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingFoodDraftItem {
    private Long bookingFoodDraftItemId;
    private Long bookingId;
    private Long foodId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}
