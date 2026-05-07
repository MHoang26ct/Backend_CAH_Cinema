package com.uit.backend_cinema.modules.ticket.domain.repository;

import com.uit.backend_cinema.modules.ticket.domain.entity.PendingTicketItem;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingTicketItemRepository {
    List<PendingTicketItem> saveAll(List<PendingTicketItem> items);

    List<PendingTicketItem> findAllActiveByBookingId(Long bookingId);

    void softDeleteByBookingId(Long bookingId);

    void hardDeleteSoftDeletedBefore(LocalDateTime threshold);
}
