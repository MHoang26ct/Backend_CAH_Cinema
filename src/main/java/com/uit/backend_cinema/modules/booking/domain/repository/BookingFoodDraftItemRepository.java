package com.uit.backend_cinema.modules.booking.domain.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingFoodDraftItem;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingFoodDraftItemRepository {
    List<BookingFoodDraftItem> saveAll(List<BookingFoodDraftItem> items);

    void softDeleteByBookingId(Long bookingId);

    void hardDeleteSoftDeletedBefore(LocalDateTime threshold);
}
