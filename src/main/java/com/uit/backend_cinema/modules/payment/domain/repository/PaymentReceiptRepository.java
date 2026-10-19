package com.uit.backend_cinema.modules.payment.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;

public interface PaymentReceiptRepository {
    PaymentReceipt save(PaymentReceipt receipt);

    Optional<PaymentReceipt> findById(Long paymentReceiptId);

    Optional<PaymentReceipt> findByGatewayAndTransactionReference(String gateway, String transactionReference);

    List<PaymentReceipt> findByBookingId(Long bookingId);

    boolean existsByGatewayAndTransactionReference(String gateway, String transactionReference);
}
