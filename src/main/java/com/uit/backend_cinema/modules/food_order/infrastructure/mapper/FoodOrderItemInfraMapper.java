package com.uit.backend_cinema.modules.food_order.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderItemJpaEntity;

@Mapper(componentModel = "spring")
public interface FoodOrderItemInfraMapper {

    @Mapping(source = "food.foodId", target = "foodId")
    @Mapping(source = "food.name", target = "foodName")
    FoodOrderItem toDomain(FoodOrderItemJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "food", ignore = true)
    @Mapping(target = "foodOrder", ignore = true)
    FoodOrderItemJpaEntity toEntity(FoodOrderItem domain);
}
