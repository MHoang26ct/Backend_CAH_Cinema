package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.PaymentConfirmationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPaymentConfirmationRepository extends JpaRepository<PaymentConfirmationJpaEntity, Long> {
    Optional<PaymentConfirmationJpaEntity> findByPaymentRef(String paymentRef);

    Optional<PaymentConfirmationJpaEntity> findByBookingIdAndStatus(
            Long bookingId,
            PaymentConfirmationStatus status
    );
}
