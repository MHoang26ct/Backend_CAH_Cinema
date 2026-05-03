package com.uit.backend_cinema.modules.food_order.infrastructure.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderItemId implements Serializable {
    private long foodOrderId;
    private long foodId;
}
