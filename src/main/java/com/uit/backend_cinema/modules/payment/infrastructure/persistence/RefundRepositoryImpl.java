package com.uit.backend_cinema.modules.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.infrastructure.entity.RefundJpaEntity;
import com.uit.backend_cinema.modules.payment.infrastructure.repository.JpaRefundRepository;

@Repository
public class RefundRepositoryImpl implements RefundRepository {

    private final JpaRefundRepository jpaRepository;

    public RefundRepositoryImpl(JpaRefundRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Refund save(Refund refund) {
        RefundJpaEntity entity = toJpaEntity(refund);
        RefundJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Refund> findById(Long refundId) {
        return jpaRepository.findById(refundId).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByRequestReference(String requestReference) {
        return jpaRepository.findByRequestReference(requestReference).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByPaymentReceiptIdAndCause(Long paymentReceiptId, RefundCause cause) {
        return jpaRepository.findByPaymentReceiptIdAndCause(paymentReceiptId, cause).map(this::toDomain);
    }

    @Override
    public List<Refund> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    private Refund toDomain(RefundJpaEntity entity) {
        if (entity == null) return null;
        return Refund.builder()
                .refundId(entity.getRefundId())
                .paymentReceiptId(entity.getPaymentReceiptId())
                .bookingId(entity.getBookingId())
                .cause(entity.getCause())
                .amount(entity.getAmount())
                .policySnapshot(entity.getPolicySnapshot())
                .status(entity.getStatus())
                .requestReference(entity.getRequestReference())
                .actorStaffId(entity.getActorStaffId())
                .claimedAt(entity.getClaimedAt())
                .processedAt(entity.getProcessedAt())
                .gatewayResponseCode(entity.getGatewayResponseCode())
                .gatewayTransactionNo(entity.getGatewayTransactionNo())
                .gatewayResponseMessage(entity.getGatewayResponseMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private RefundJpaEntity toJpaEntity(Refund domain) {
        if (domain == null) return null;
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setRefundId(domain.getRefundId());
        entity.setPaymentReceiptId(domain.getPaymentReceiptId());
        entity.setBookingId(domain.getBookingId());
        entity.setCause(domain.getCause());
        entity.setAmount(domain.getAmount());
        entity.setPolicySnapshot(domain.getPolicySnapshot());
        entity.setStatus(domain.getStatus());
        entity.setRequestReference(domain.getRequestReference());
        entity.setActorStaffId(domain.getActorStaffId());
        entity.setClaimedAt(domain.getClaimedAt());
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setGatewayResponseCode(domain.getGatewayResponseCode());
        entity.setGatewayTransactionNo(domain.getGatewayTransactionNo());
        entity.setGatewayResponseMessage(domain.getGatewayResponseMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
