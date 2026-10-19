package com.uit.backend_cinema.modules.payment.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;

public interface RefundRepository {
    Refund save(Refund refund);

    Optional<Refund> findById(Long refundId);

    /** Locks one refund while applying or reversing its membership adjustment. */
    Optional<Refund> findByIdForUpdate(Long refundId);

    Optional<Refund> findByRequestReference(String requestReference);

    Optional<Refund> findByPaymentReceiptIdAndCause(Long paymentReceiptId, RefundCause cause);

    List<Refund> findByBookingId(Long bookingId);

    List<Refund> findByStatus(RefundStatus status);

    boolean claimCashRefund(Long refundId, Long staffId, java.time.LocalDateTime claimedAt);

    boolean confirmCashPayout(Long refundId, Long staffId, java.time.LocalDateTime processedAt);
}
