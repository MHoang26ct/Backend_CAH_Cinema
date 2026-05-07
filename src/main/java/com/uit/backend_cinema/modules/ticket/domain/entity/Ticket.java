package com.uit.backend_cinema.modules.ticket.domain.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ticket {
    private Long ticketId;
    private Long seatId;
    private Long showtimeId;
    private Long bookingId;
    private BigDecimal price;
}
