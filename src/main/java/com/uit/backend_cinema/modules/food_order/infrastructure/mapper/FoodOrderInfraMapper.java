package com.uit.backend_cinema.modules.food_order.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderJpaEntity;

@Mapper(componentModel = "spring", uses = { FoodOrderItemInfraMapper.class })
public interface FoodOrderInfraMapper {
    FoodOrder toDomain(FoodOrderJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "foodOrderId", ignore = true)
    @Mapping(target = "items", ignore = true)
    FoodOrderJpaEntity toEntity(FoodOrder domain);
}
