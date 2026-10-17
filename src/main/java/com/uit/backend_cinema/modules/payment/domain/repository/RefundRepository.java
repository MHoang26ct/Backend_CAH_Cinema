package com.uit.backend_cinema.modules.payment.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;

public interface RefundRepository {
    Refund save(Refund refund);

    Optional<Refund> findById(Long refundId);

    Optional<Refund> findByRequestReference(String requestReference);

    Optional<Refund> findByPaymentReceiptIdAndCause(Long paymentReceiptId, RefundCause cause);

    List<Refund> findByBookingId(Long bookingId);

    List<Refund> findByStatus(RefundStatus status);
}
