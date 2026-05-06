package com.uit.backend_cinema.modules.booking.infrastructure.mapper;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingFoodDraftItem;
import com.uit.backend_cinema.modules.booking.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingFoodDraftItemJpaEntity;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.PendingTicketItemJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingInfraMapper {
    Booking toDomain(BookingJpaEntity entity);

    BookingJpaEntity toEntity(Booking booking);

    PendingTicketItem toDomain(PendingTicketItemJpaEntity entity);

    PendingTicketItemJpaEntity toEntity(PendingTicketItem item);

    BookingFoodDraftItem toDomain(BookingFoodDraftItemJpaEntity entity);

    BookingFoodDraftItemJpaEntity toEntity(BookingFoodDraftItem item);

    @AfterMapping
    default void normalizeBooking(Booking booking, @MappingTarget BookingJpaEntity entity) {
        if (booking.getIsDeleted() == null) {
            entity.setIsDeleted(false);
        }
    }

    @AfterMapping
    default void normalizePendingTicketItem(PendingTicketItem item, @MappingTarget PendingTicketItemJpaEntity entity) {
        if (item.getIsDeleted() == null) {
            entity.setIsDeleted(false);
        }
    }

    @AfterMapping
    default void normalizeBookingFoodDraftItem(BookingFoodDraftItem item, @MappingTarget BookingFoodDraftItemJpaEntity entity) {
        if (item.getIsDeleted() == null) {
            entity.setIsDeleted(false);
        }
    }
}
