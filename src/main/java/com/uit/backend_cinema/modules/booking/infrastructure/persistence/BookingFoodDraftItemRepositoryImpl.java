package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingFoodDraftItem;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingFoodDraftItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookingFoodDraftItemRepositoryImpl implements BookingFoodDraftItemRepository {
    private final JpaBookingFoodDraftItemRepository jpaBookingFoodDraftItemRepository;
    private final BookingInfraMapper mapper;

    public BookingFoodDraftItemRepositoryImpl(JpaBookingFoodDraftItemRepository jpaBookingFoodDraftItemRepository,
                                              BookingInfraMapper mapper) {
        this.jpaBookingFoodDraftItemRepository = jpaBookingFoodDraftItemRepository;
        this.mapper = mapper;
    }

    @Override
    public List<BookingFoodDraftItem> saveAll(List<BookingFoodDraftItem> items) {
        return jpaBookingFoodDraftItemRepository.saveAll(items.stream().map(mapper::toEntity).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void softDeleteByBookingId(Long bookingId) {
        jpaBookingFoodDraftItemRepository.softDeleteByBookingId(bookingId);
    }

    @Override
    public void hardDeleteSoftDeletedBefore(LocalDateTime threshold) {
        jpaBookingFoodDraftItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }
}
