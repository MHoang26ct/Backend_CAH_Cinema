package com.uit.backend_cinema.modules.food_order.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    public List<Food> getAll() {
        return foodRepository.getAllFoods();
    }

    @Transactional
    public Food create(Food food) {
        return foodRepository.save(food);
    }


    @Transactional
    public void delete(Food food) {
        Food existingFood = foodRepository.findById(food.getFoodId())
                .orElseThrow(() -> new BusinessException(
                        "Thức ăn/Thức uống với Id " + food.getFoodId() + " không tồn tại",
                        ErrorCode.RESOURCE_NOT_FOUND));
        existingFood.setDeleted(true);
        foodRepository.save(existingFood);
    }
}
