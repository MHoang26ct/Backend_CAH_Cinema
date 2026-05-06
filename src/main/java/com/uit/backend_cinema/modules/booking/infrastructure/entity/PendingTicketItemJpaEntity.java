package com.uit.backend_cinema.modules.booking.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pending_ticket_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "seat_id"})
)
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PendingTicketItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_ticket_item_id")
    private Long pendingTicketItemId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
