package com.uit.backend_cinema.modules.food_order.api.mapper;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoodApiMapper {
    FoodDTO toDTO(Food food);
}
