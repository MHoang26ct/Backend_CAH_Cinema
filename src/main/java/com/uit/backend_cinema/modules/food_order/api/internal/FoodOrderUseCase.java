package com.uit.backend_cinema.modules.food_order.api.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderResponseDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.OrderFoodRequestDTO;
import com.uit.backend_cinema.modules.food_order.api.mapper.FoodOrderApiMapper;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;

@Service
@Validated
public class FoodOrderUseCase {

    private final FoodOrderService foodOrderService;

    private final FoodOrderApiMapper mapper;

    public FoodOrderUseCase(FoodOrderService foodOrderService, FoodOrderApiMapper mapper) {
        this.foodOrderService = foodOrderService;
        this.mapper = mapper;
    }

    public BigDecimal orderFood(@Valid OrderFoodRequestDTO requestDTO) {
        return foodOrderService.createFoodOrder(mapper.toDomain(requestDTO));
    }

    public List<FoodOrderResponseDTO> getFoodOrdersByBookingIds(Set<Long> bookingIds) {
        return foodOrderService.getFoodOrdersByBookingIds(bookingIds).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
