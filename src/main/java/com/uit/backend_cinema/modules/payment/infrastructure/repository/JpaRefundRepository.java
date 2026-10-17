package com.uit.backend_cinema.modules.payment.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.infrastructure.entity.RefundJpaEntity;

@Repository
public interface JpaRefundRepository extends JpaRepository<RefundJpaEntity, Long> {
    Optional<RefundJpaEntity> findByRequestReference(String requestReference);

    Optional<RefundJpaEntity> findByPaymentReceiptIdAndCause(Long paymentReceiptId, RefundCause cause);

    List<RefundJpaEntity> findByBookingId(Long bookingId);

    List<RefundJpaEntity> findByStatus(RefundStatus status);
}
