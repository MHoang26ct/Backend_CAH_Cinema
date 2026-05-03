package com.uit.backend_cinema.modules.food_order.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FoodOrderService {
    private final FoodService foodService;
    private final FoodOrderRepository foodOrderRepository;

    public FoodOrderService(FoodService foodService, FoodOrderRepository foodOrderRepository) {
        this.foodService = foodService;
        this.foodOrderRepository = foodOrderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<FoodOrder> getByBookingId(long bookingId) {
        return foodOrderRepository.getByBookingId(bookingId);
    }

    @Transactional
    public BigDecimal createFoodOrder(FoodOrder order) {
        List<FoodOrderItem> items = order.getItems();
        List<Food> foods = foodService.findAllByListId(items.stream().map(FoodOrderItem::getFoodId).collect(Collectors.toSet()));
        Map<Long, Food> foodMap = foods.stream().collect(Collectors.toMap(Food::getFoodId, f -> f));
        for (FoodOrderItem item : items) {
            Food food = foodMap.get(item.getFoodId());
            if (!food.isAvailable()) {
                throw new BusinessException("Món ăn " + food.getName() + " không còn trong kho", ErrorCode.VALIDATION_FAILED);
            }
            item.setPrice(foodMap.get(item.getFoodId()).getPrice());
        }
        order.setTotalPrice(items.stream().map(item -> item.getPrice()
                .multiply(new BigDecimal(item.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add));
        foodOrderRepository.save(order);
        return order.getTotalPrice();
    }

    @Transactional(readOnly = true)
    public List<FoodOrder> getFoodOrdersByBookingIds(Set<Long> bookingIds) {
        return foodOrderRepository.getAllByListBookingId(bookingIds);
    }
}
