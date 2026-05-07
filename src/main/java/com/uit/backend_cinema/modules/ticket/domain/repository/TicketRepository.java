package com.uit.backend_cinema.modules.ticket.domain.repository;

import java.util.List;

import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;

public interface TicketRepository {
    boolean existsByBookingIdAndSeatId(Long bookingId, Long seatId);

    int saveAll(List<Ticket> tickets);

    List<Ticket> findAllByBookingId(Long bookingId);

    boolean existsSoldSeatByShowtimeIdAndSeatIds(Long showtimeId, List<Long> seatIds);

    List<Long> findSoldSeatIdsByShowtimeId(Long showtimeId);
}
