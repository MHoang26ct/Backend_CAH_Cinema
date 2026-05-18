package com.uit.backend_cinema.modules.food_order.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodDTO;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodRequestDTO;
import com.uit.backend_cinema.modules.food_order.api.mapper.FoodApiMapper;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/food")
public class AdminFoodController {

    private final FoodService foodService;
    private final FoodApiMapper mapper;

    public AdminFoodController(FoodService foodService, FoodApiMapper mapper) {
        this.foodService = foodService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getAllFoods() {
        List<FoodDTO> response = foodService.getAll().stream()
                .map(mapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách tất cả thức ăn thành công"));
    }

    @PostMapping
    public ResponseEntity<?> createFood(@Valid @RequestBody FoodRequestDTO request) {
        Food newFood = mapper.toDomain(request);
        Food createdFood = foodService.create(newFood);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mapper.toDTO(createdFood), "Tạo mới thức ăn thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFood(@PathVariable("id") long id, @Valid @RequestBody FoodRequestDTO request) {
        Food updatedFood = foodService.update(id, mapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.success(mapper.toDTO(updatedFood), "Cập nhật thức ăn thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFood(@PathVariable("id") long id) {
        foodService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thức ăn thành công"));
    }
}
