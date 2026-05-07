package com.uit.backend_cinema.modules.food_order.infrastructure.persistence;

import com.uit.backend_cinema.modules.food_order.domain.entity.BookingFoodDraftItem;
import com.uit.backend_cinema.modules.food_order.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.BookingFoodDraftItemJpaEntity;
import com.uit.backend_cinema.modules.food_order.infrastructure.repository.JpaBookingFoodDraftItemRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookingFoodDraftItemRepositoryImpl implements BookingFoodDraftItemRepository {
    private final JpaBookingFoodDraftItemRepository jpaBookingFoodDraftItemRepository;

    public BookingFoodDraftItemRepositoryImpl(JpaBookingFoodDraftItemRepository jpaBookingFoodDraftItemRepository) {
        this.jpaBookingFoodDraftItemRepository = jpaBookingFoodDraftItemRepository;
    }

    @Override
    public List<BookingFoodDraftItem> findAllActiveByBookingId(Long bookingId) {
        return jpaBookingFoodDraftItemRepository.findByBookingId(bookingId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<BookingFoodDraftItem> saveAll(List<BookingFoodDraftItem> items) {
        return jpaBookingFoodDraftItemRepository.saveAll(items.stream().map(this::toEntity).toList())
                .stream()
                .map(this::toDomain)
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

    private BookingFoodDraftItem toDomain(BookingFoodDraftItemJpaEntity entity) {
        BookingFoodDraftItem item = new BookingFoodDraftItem();
        item.setBookingFoodDraftItemId(entity.getBookingFoodDraftItemId());
        item.setBookingId(entity.getBookingId());
        item.setFoodId(entity.getFoodId());
        item.setQuantity(entity.getQuantity());
        item.setUnitPrice(entity.getUnitPrice());
        item.setIsDeleted(entity.getIsDeleted());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    private BookingFoodDraftItemJpaEntity toEntity(BookingFoodDraftItem item) {
        BookingFoodDraftItemJpaEntity entity = new BookingFoodDraftItemJpaEntity();
        entity.setBookingFoodDraftItemId(item.getBookingFoodDraftItemId());
        entity.setBookingId(item.getBookingId());
        entity.setFoodId(item.getFoodId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setIsDeleted(item.getIsDeleted() == null ? false : item.getIsDeleted());
        return entity;
    }
}
