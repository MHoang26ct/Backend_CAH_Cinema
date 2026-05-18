package com.uit.backend_cinema.modules.food_order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodRepository;
import com.uit.backend_cinema.modules.food_order.infrastructure.mapper.FoodInfraMapper;
import com.uit.backend_cinema.modules.food_order.infrastructure.repository.JpaFoodRepository;

@Repository
public class FoodRepositoryImpl implements FoodRepository {
    private final JpaFoodRepository jpaFoodRepository;
    private final FoodInfraMapper mapper;

    public FoodRepositoryImpl(JpaFoodRepository jpaFoodRepository, FoodInfraMapper mapper) {
        this.mapper = mapper;
        this.jpaFoodRepository = jpaFoodRepository;
    }

    @Override
    public Optional<Food> findById(long id) {
        return jpaFoodRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Food> findAllByListId(Set<Long> ids) {
        return jpaFoodRepository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Food> getAllAvailableFoods() {
        return jpaFoodRepository.findByAvailableTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Food> getAllFoods() {
        return jpaFoodRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Food save(Food food) {
        return mapper.toDomain(jpaFoodRepository.save(mapper.toEntity(food)));
    }

    @Override
    public void delete(Food food) {
        jpaFoodRepository.save(mapper.toEntity(food));
    }
}
