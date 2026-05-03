package com.uit.backend_cinema.modules.food_order.api.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderFoodRequestDTO {
    @NotNull(message = "Mã booking không được null")
    private Long bookingId;

    @NotEmpty(message = "Danh sách food không được trống")
    private List<FoodOrderItemRequestDTO> items;
}
