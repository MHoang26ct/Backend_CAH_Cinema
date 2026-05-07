package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.PaymentConfirmationJpaEntity;
import com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaPaymentConfirmationRepository;

@Repository
public class PaymentConfirmationRepositoryImpl implements PaymentConfirmationRepository {
    private final JpaPaymentConfirmationRepository jpaPaymentConfirmationRepository;
    private final BookingInfraMapper bookingInfraMapper;

    public PaymentConfirmationRepositoryImpl(JpaPaymentConfirmationRepository jpaPaymentConfirmationRepository, BookingInfraMapper bookingInfraMapper) {
        this.jpaPaymentConfirmationRepository = jpaPaymentConfirmationRepository;
        this.bookingInfraMapper = bookingInfraMapper;
    }

    @Override
    public PaymentConfirmation save(PaymentConfirmation paymentConfirmation) {
        PaymentConfirmationJpaEntity entity = bookingInfraMapper.toEntity(paymentConfirmation);
        return bookingInfraMapper.toDomain(jpaPaymentConfirmationRepository.save(entity));
    }

    @Override
    public Optional<PaymentConfirmation> findByPaymentRef(String paymentRef) {
        return jpaPaymentConfirmationRepository.findByPaymentRef(paymentRef)
                .map(bookingInfraMapper::toDomain);
    }

    @Override
    public Optional<PaymentConfirmation> findByBookingIdAndStatus(Long bookingId, PaymentConfirmationStatus status) {
        return jpaPaymentConfirmationRepository.findByBookingIdAndStatus(bookingId, status)
                .map(bookingInfraMapper::toDomain);
    }
}
