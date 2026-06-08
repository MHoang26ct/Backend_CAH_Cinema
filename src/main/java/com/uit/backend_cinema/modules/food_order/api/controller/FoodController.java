package com.uit.backend_cinema.modules.food_order.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodDTO;
import com.uit.backend_cinema.modules.food_order.api.mapper.FoodApiMapper;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodService;

@RestController
@RequestMapping("/api/v1/foods")
public class FoodController {

    private final FoodService foodService;
    private final FoodApiMapper mapper;

    public FoodController(FoodService foodOrderService, FoodApiMapper mapper) {
        this.foodService = foodOrderService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getAllFoodOrder() {
        List<FoodDTO> response = foodService.getAvailableFoods().stream()
                .map(mapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thức ăn thành công"));
    }
}
