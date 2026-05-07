package com.uit.backend_cinema.modules.food_order.domain.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Food {
    private long foodId;
    private String name;
    private String description;
    private BigDecimal price;
    private Category category;
    private String imageUrl;
    private boolean available = true;
    private boolean deleted = false;
}
