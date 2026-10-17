package com.uit.backend_cinema.modules.payment.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentReceiptJpaEntity;

@Repository
public interface JpaPaymentReceiptRepository extends JpaRepository<PaymentReceiptJpaEntity, Long> {
    Optional<PaymentReceiptJpaEntity> findByGatewayAndTransactionReference(String gateway, String transactionReference);

    List<PaymentReceiptJpaEntity> findByBookingId(Long bookingId);

    boolean existsByGatewayAndTransactionReference(String gateway, String transactionReference);
}
