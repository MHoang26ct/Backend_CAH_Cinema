package com.uit.backend_cinema.modules.food_order.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uit.backend_cinema.modules.food_order.domain.entity.Food;

public interface FoodRepository {
    Optional<Food> findById(long id);

    List<Food> findAllByListId(Set<Long> ids);
    List<Food> getAllAvailableFoods();
    List<Food> getAllFoods();
    Food save(Food food);

    void delete(Food food);
}
