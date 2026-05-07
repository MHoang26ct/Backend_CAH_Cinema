package com.uit.backend_cinema.modules.booking.infrastructure.mapper;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.PaymentConfirmationJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingInfraMapper {
    Booking toDomain(BookingJpaEntity entity);

    BookingJpaEntity toEntity(Booking booking);

    PaymentConfirmation toDomain(PaymentConfirmationJpaEntity entity);

    PaymentConfirmationJpaEntity toEntity(PaymentConfirmation paymentConfirmation);

    @AfterMapping
    default void normalizeBooking(Booking booking, @MappingTarget BookingJpaEntity entity) {
        if (booking.getIsDeleted() == null) {
            entity.setIsDeleted(false);
        }
    }
}
