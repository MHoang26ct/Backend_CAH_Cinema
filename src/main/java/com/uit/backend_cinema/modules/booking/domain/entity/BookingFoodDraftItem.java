package com.uit.backend_cinema.modules.booking.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
