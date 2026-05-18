package com.uit.backend_cinema.modules.food_order.api.entity;

import java.math.BigDecimal;

import com.uit.backend_cinema.modules.food_order.domain.entity.Category;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodRequestDTO {
    @NotBlank(message = "Tên thức ăn không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá tiền không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá tiền phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Danh mục không được để trống")
    private Category category;

    private String imageUrl;

    private boolean available = true;
}
