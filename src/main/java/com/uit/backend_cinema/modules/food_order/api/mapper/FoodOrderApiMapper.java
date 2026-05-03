package com.uit.backend_cinema.modules.food_order.api.mapper;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderResponseDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.OrderFoodRequestDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FoodOrderItemApiMapper.class})
public interface FoodOrderApiMapper {
    FoodOrderResponseDTO toResponse(FoodOrder foodOrder);

    FoodOrder toDomain(OrderFoodRequestDTO foodOrderDTO);
}
