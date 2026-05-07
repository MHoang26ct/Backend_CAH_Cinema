package com.uit.backend_cinema.modules.ticket.infrastructure.persistence;

import com.uit.backend_cinema.modules.ticket.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.infrastructure.entity.PendingTicketItemJpaEntity;
import com.uit.backend_cinema.modules.ticket.infrastructure.repository.JpaPendingTicketItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PendingTicketItemRepositoryImpl implements PendingTicketItemRepository {
    private final JpaPendingTicketItemRepository jpaPendingTicketItemRepository;

    public PendingTicketItemRepositoryImpl(JpaPendingTicketItemRepository jpaPendingTicketItemRepository) {
        this.jpaPendingTicketItemRepository = jpaPendingTicketItemRepository;
    }

    @Override
    public List<PendingTicketItem> saveAll(List<PendingTicketItem> items) {
        return jpaPendingTicketItemRepository.saveAll(items.stream().map(this::toEntity).toList())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PendingTicketItem> findAllActiveByBookingId(Long bookingId) {
        return jpaPendingTicketItemRepository.findByBookingId(bookingId).stream().map(this::toDomain).toList();
    }

    @Override
    public void softDeleteByBookingId(Long bookingId) {
        jpaPendingTicketItemRepository.softDeleteByBookingId(bookingId);
    }

    @Override
    public void hardDeleteSoftDeletedBefore(LocalDateTime threshold) {
        jpaPendingTicketItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }

    private PendingTicketItem toDomain(PendingTicketItemJpaEntity entity) {
        PendingTicketItem item = new PendingTicketItem();
        item.setPendingTicketItemId(entity.getPendingTicketItemId());
        item.setBookingId(entity.getBookingId());
        item.setSeatId(entity.getSeatId());
        item.setUnitPrice(entity.getUnitPrice());
        item.setIsDeleted(entity.getIsDeleted());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    private PendingTicketItemJpaEntity toEntity(PendingTicketItem item) {
        PendingTicketItemJpaEntity entity = new PendingTicketItemJpaEntity();
        entity.setPendingTicketItemId(item.getPendingTicketItemId());
        entity.setBookingId(item.getBookingId());
        entity.setSeatId(item.getSeatId());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setIsDeleted(item.getIsDeleted() == null ? false : item.getIsDeleted());
        return entity;
    }
}
