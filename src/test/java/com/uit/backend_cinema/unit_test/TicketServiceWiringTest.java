package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;

class TicketServiceWiringTest {
    @Test
    void springSelectsProductionConstructorAndInjectsSeatRepository() {
        TicketRepository tickets = mock(TicketRepository.class);
        PendingTicketItemRepository drafts = mock(PendingTicketItemRepository.class);
        SeatRepository seats = mock(SeatRepository.class);

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setBookingId(2L);
        ticket.setSeatId(3L);
        ticket.setCheckinCode("TK-ABCD-EFGH");
        Seat seat = new Seat();
        seat.setSeatId(3L);
        seat.setSeatRow(BigDecimal.ONE);
        seat.setSeatCol(BigDecimal.valueOf(8));
        when(tickets.findAllByBookingId(2L)).thenReturn(List.of(ticket));
        when(seats.findByIds(List.of(3L))).thenReturn(List.of(seat));

        // Use Spring's constructor selection, not a manual new TicketService(...).
        // This catches wiring errors without requiring Docker or an external DB.
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TicketRepository.class, () -> tickets);
            context.registerBean(PendingTicketItemRepository.class, () -> drafts);
            context.registerBean(SeatRepository.class, () -> seats);
            context.register(TicketService.class);
            context.refresh();

            var result = context.getBean(TicketService.class).getTicketDetailsByBookingId(2L);
            assertEquals(1, result.size());
            assertEquals("A8", result.get(0).getSeatName());
            verify(seats).findByIds(List.of(3L));
        }
    }
}
