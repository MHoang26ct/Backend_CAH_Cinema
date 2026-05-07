package com.uit.backend_cinema.modules.ticket.domain.repository;

import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;

import java.util.List;

public interface TicketRepository {
    boolean existsByBookingIdAndSeatId(Long bookingId, Long seatId);

    int saveAll(List<Ticket> tickets);

    List<Ticket> findAllByBookingId(Long bookingId);
}
