package com.uit.backend_cinema.modules.food_order.api.mapper;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemResponseDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FoodOrderItemApiMapper {
    @Mapping(target = "total", expression = "java(foodOrderItem.getPrice().multiply(new java.math.BigDecimal(foodOrderItem.getQuantity())))")
    FoodOrderItemResponseDTO toResponseDTO(FoodOrderItem foodOrderItem);

    FoodOrderItem toDomain(FoodOrderItemRequestDTO foodOrderItemDTO);
}
