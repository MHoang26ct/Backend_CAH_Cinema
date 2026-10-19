package com.uit.backend_cinema.modules.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentReceiptJpaEntity;
import com.uit.backend_cinema.modules.payment.infrastructure.repository.JpaPaymentReceiptRepository;

@Repository
public class PaymentReceiptRepositoryImpl implements PaymentReceiptRepository {

    private final JpaPaymentReceiptRepository jpaRepository;

    public PaymentReceiptRepositoryImpl(JpaPaymentReceiptRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentReceipt save(PaymentReceipt receipt) {
        PaymentReceiptJpaEntity entity = toJpaEntity(receipt);
        PaymentReceiptJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PaymentReceipt> findById(Long paymentReceiptId) {
        return jpaRepository.findById(paymentReceiptId).map(this::toDomain);
    }

    @Override
    public Optional<PaymentReceipt> findByGatewayAndTransactionReference(String gateway, String transactionReference) {
        return jpaRepository.findByGatewayAndTransactionReference(gateway, transactionReference).map(this::toDomain);
    }

    @Override
    public List<PaymentReceipt> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByGatewayAndTransactionReference(String gateway, String transactionReference) {
        return jpaRepository.existsByGatewayAndTransactionReference(gateway, transactionReference);
    }

    private PaymentReceipt toDomain(PaymentReceiptJpaEntity entity) {
        if (entity == null) return null;
        return PaymentReceipt.builder()
                .paymentReceiptId(entity.getPaymentReceiptId())
                .bookingId(entity.getBookingId())
                .paymentRequestId(entity.getPaymentRequestId())
                .gateway(entity.getGateway())
                .transactionReference(entity.getTransactionReference())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .payDate(entity.getPayDate())
                .verifiedAt(entity.getVerifiedAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PaymentReceiptJpaEntity toJpaEntity(PaymentReceipt domain) {
        if (domain == null) return null;
        PaymentReceiptJpaEntity entity = new PaymentReceiptJpaEntity();
        entity.setPaymentReceiptId(domain.getPaymentReceiptId());
        entity.setBookingId(domain.getBookingId());
        entity.setPaymentRequestId(domain.getPaymentRequestId());
        entity.setGateway(domain.getGateway());
        entity.setTransactionReference(domain.getTransactionReference());
        entity.setOrderId(domain.getOrderId());
        entity.setAmount(domain.getAmount());
        entity.setPayDate(domain.getPayDate());
        entity.setVerifiedAt(domain.getVerifiedAt());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
