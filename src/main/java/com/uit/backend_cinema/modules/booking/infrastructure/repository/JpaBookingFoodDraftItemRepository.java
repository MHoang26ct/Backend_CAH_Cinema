package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingFoodDraftItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface JpaBookingFoodDraftItemRepository extends JpaRepository<BookingFoodDraftItemJpaEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookingFoodDraftItemJpaEntity f set f.isDeleted = true where f.bookingId = :bookingId and f.isDeleted = false")
    void softDeleteByBookingId(@Param("bookingId") Long bookingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookingFoodDraftItemJpaEntity f where f.isDeleted = true and f.createdAt < :threshold")
    void hardDeleteSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);

    List<BookingFoodDraftItemJpaEntity> findByBookingId(Long bookingId);
}
