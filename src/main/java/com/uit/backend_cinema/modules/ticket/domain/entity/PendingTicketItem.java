package com.uit.backend_cinema.modules.ticket.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingTicketItem {
    private Long pendingTicketItemId;
    private Long bookingId;
    private Long seatId;
    private BigDecimal unitPrice;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}
