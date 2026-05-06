package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import com.uit.backend_cinema.modules.booking.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.booking.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaPendingTicketItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PendingTicketItemRepositoryImpl implements PendingTicketItemRepository {
    private final JpaPendingTicketItemRepository jpaPendingTicketItemRepository;
    private final BookingInfraMapper mapper;

    public PendingTicketItemRepositoryImpl(JpaPendingTicketItemRepository jpaPendingTicketItemRepository,
                                           BookingInfraMapper mapper) {
        this.jpaPendingTicketItemRepository = jpaPendingTicketItemRepository;
        this.mapper = mapper;
    }

    @Override
    public List<PendingTicketItem> saveAll(List<PendingTicketItem> items) {
        return jpaPendingTicketItemRepository.saveAll(items.stream().map(mapper::toEntity).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PendingTicketItem> findAllActiveByBookingId(Long bookingId) {
        return jpaPendingTicketItemRepository.findByBookingId(bookingId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void softDeleteByBookingId(Long bookingId) {
        jpaPendingTicketItemRepository.softDeleteByBookingId(bookingId);
    }

    @Override
    public void hardDeleteSoftDeletedBefore(LocalDateTime threshold) {
        jpaPendingTicketItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }
}
