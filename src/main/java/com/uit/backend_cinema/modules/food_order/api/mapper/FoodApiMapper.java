package com.uit.backend_cinema.modules.food_order.api.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;

@Mapper(componentModel = "spring")
public interface FoodApiMapper {
    FoodDTO toDTO(Food food);
}
