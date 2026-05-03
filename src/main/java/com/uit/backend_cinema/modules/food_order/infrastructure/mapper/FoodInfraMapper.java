package com.uit.backend_cinema.modules.food_order.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodJpaEntity;

@Mapper(componentModel = "spring")
public interface FoodInfraMapper {
    Food toDomain(FoodJpaEntity entity);

    FoodJpaEntity toEntity(Food domain);
}
