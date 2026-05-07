package com.uit.backend_cinema.modules.food_order.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.uit.backend_cinema.modules.food_order.domain.entity.BookingFoodDraftItem;

public interface BookingFoodDraftItemRepository {
    List<BookingFoodDraftItem> saveAll(List<BookingFoodDraftItem> items);
    List<BookingFoodDraftItem> findAllActiveByBookingId(Long bookingId);

    void softDeleteByBookingId(Long bookingId);

    void hardDeleteSoftDeletedBefore(LocalDateTime threshold);
}
