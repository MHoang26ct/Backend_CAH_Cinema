package com.uit.backend_cinema.modules.ticket.infrastructure.repository;

import com.uit.backend_cinema.modules.ticket.infrastructure.entity.TicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTicketRepository extends JpaRepository<TicketJpaEntity, Long> {
    boolean existsByBookingIdAndSeatId(Long bookingId, Long seatId);

    List<TicketJpaEntity> findByBookingId(Long bookingId);
}
