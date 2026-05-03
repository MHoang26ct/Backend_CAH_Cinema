package com.uit.backend_cinema.modules.food_order.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;

public interface FoodOrderRepository {
    List<FoodOrder> getAllByListBookingId(Set<Long> bookingId);
    Optional<FoodOrder> getByBookingId(long bookingId);

    FoodOrder save(FoodOrder foodOrder);
}
