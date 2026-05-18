package com.uit.backend_cinema.modules.food_order.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodJpaEntity;

@Repository
public interface JpaFoodRepository extends JpaRepository<FoodJpaEntity, Long> {
    List<FoodJpaEntity> findByAvailableTrue();
}
