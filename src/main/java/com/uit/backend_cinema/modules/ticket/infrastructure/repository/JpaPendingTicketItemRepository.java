package com.uit.backend_cinema.modules.ticket.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.ticket.infrastructure.entity.PendingTicketItemJpaEntity;

public interface JpaPendingTicketItemRepository extends JpaRepository<PendingTicketItemJpaEntity, Long> {
    List<PendingTicketItemJpaEntity> findByBookingId(Long bookingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PendingTicketItemJpaEntity p set p.isDeleted = true where p.bookingId = :bookingId and p.isDeleted = false")
    void softDeleteByBookingId(@Param("bookingId") Long bookingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PendingTicketItemJpaEntity p where p.isDeleted = true and p.createdAt < :threshold")
    void hardDeleteSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);
}
