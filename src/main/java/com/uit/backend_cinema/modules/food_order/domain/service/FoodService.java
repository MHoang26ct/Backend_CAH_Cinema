package com.uit.backend_cinema.modules.food_order.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodRepository;

@Service
@Transactional(readOnly = true)
public class FoodService {
    private final FoodRepository foodRepository;
    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    public List<Food> findAllByListId(Set<Long> ids) {
        List<Food> foodList = foodRepository.findAllByListId(ids);
        if (foodList.size() != ids.size()) {
            throw new BusinessException("Một hoặc nhiều thức ăn/thức uống không tồn tại", ErrorCode.RESOURCE_NOT_FOUND);
        }
        return foodList;
    }

    public List<Food> getAvailableFoods() {
        return foodRepository.getAllAvailableFoods();
    }

    public List<Food> getAll() {
        return foodRepository.getAllFoods();
    }

    @Transactional
    public Food create(Food food) {
        return foodRepository.save(food);
    }

    @Transactional
    public Food update(long id, Food updateData) {
        Food existingFood = foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Thức ăn/Thức uống với Id " + id + " không tồn tại",
                        ErrorCode.RESOURCE_NOT_FOUND));

        existingFood.setName(updateData.getName());
        existingFood.setDescription(updateData.getDescription());
        existingFood.setPrice(updateData.getPrice());
        existingFood.setCategory(updateData.getCategory());
        existingFood.setImageUrl(updateData.getImageUrl());
        existingFood.setAvailable(updateData.isAvailable());

        return foodRepository.save(existingFood);
    }

    @Transactional
    public void delete(long id) {
        Food existingFood = foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Thức ăn/Thức uống với Id " + id + " không tồn tại",
                        ErrorCode.RESOURCE_NOT_FOUND));
        existingFood.setDeleted(true);
        foodRepository.save(existingFood);
    }
}
