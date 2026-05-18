package com.uit.backend_cinema.modules.food_order.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodRequestDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;

@Mapper(componentModel = "spring")
public interface FoodApiMapper {
    FoodDTO toDTO(Food food);

    @Mapping(target = "foodId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Food toDomain(FoodRequestDTO dto);

    @Mapping(target = "foodId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateFoodFromDTO(FoodRequestDTO dto, @MappingTarget Food food);
}
