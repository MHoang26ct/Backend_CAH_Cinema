package com.uit.backend_cinema.modules.food_order.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.food_order.infrastructure.entity.BookingFoodDraftItemJpaEntity;

public interface JpaBookingFoodDraftItemRepository extends JpaRepository<BookingFoodDraftItemJpaEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookingFoodDraftItemJpaEntity f set f.isDeleted = true where f.bookingId = :bookingId and f.isDeleted = false")
    void softDeleteByBookingId(@Param("bookingId") Long bookingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from booking_food_draft_items where is_deleted = true and created_at < :threshold", nativeQuery = true)
    void hardDeleteSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);

    List<BookingFoodDraftItemJpaEntity> findByBookingId(Long bookingId);
}
