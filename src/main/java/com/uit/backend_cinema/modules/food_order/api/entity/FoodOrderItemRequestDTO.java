package com.uit.backend_cinema.modules.food_order.api.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodOrderItemRequestDTO {
    @NotNull(message = "Id không được trống")
    private Long foodId;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;
}
