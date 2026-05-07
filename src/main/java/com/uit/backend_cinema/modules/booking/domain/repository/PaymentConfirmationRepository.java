package com.uit.backend_cinema.modules.booking.domain.repository;

import java.util.Optional;

import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;

public interface PaymentConfirmationRepository {
    PaymentConfirmation save(PaymentConfirmation paymentConfirmation);

    Optional<PaymentConfirmation> findByPaymentRef(String paymentRef);

    Optional<PaymentConfirmation> findByBookingIdAndStatus(
            Long bookingId,
            PaymentConfirmationStatus status
    );
}
