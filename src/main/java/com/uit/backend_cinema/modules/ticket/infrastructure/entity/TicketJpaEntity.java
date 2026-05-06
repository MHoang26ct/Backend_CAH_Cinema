package com.uit.backend_cinema.modules.ticket.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tickets_booking_seat",
                columnNames = {"booking_id", "seat_id"}
        )
)
@Getter
@Setter
public class TicketJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}
