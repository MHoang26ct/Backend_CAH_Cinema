package com.uit.backend_cinema.modules.food_order.api.entity;

import java.math.BigDecimal;

import com.uit.backend_cinema.modules.food_order.domain.entity.Category;
import lombok.Data;

@Data
public class FoodDTO {
    private long foodId;
    private String name;
    private String description;
    private BigDecimal price;
    private Category category;
    private String imageUrl;
    private boolean available = true;
}
