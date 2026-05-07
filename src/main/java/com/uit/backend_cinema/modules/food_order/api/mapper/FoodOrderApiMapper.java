package com.uit.backend_cinema.modules.food_order.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderResponseDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.OrderFoodRequestDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;

@Mapper(componentModel = "spring", uses = {FoodOrderItemApiMapper.class})
public interface FoodOrderApiMapper {
    FoodOrderResponseDTO toResponse(FoodOrder foodOrder);

    @Mapping(target = "foodOrderId", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FoodOrder toDomain(OrderFoodRequestDTO foodOrderDTO);
}
