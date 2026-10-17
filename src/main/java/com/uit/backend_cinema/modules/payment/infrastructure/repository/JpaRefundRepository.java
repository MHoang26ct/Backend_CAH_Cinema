package com.uit.backend_cinema.modules.payment.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefundJpaEntity r
        SET r.status = :newStatus, r.actorStaffId = :staffId, r.claimedAt = :claimedAt
        WHERE r.refundId = :refundId
          AND (r.status = 'REQUESTED' OR (r.status = 'PROCESSING' AND r.actorStaffId = :staffId))
    """)
    int claimCashRefund(@Param("refundId") Long refundId,
                        @Param("staffId") Long staffId,
                        @Param("claimedAt") LocalDateTime claimedAt,
                        @Param("newStatus") RefundStatus newStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefundJpaEntity r
        SET r.status = 'SUCCEEDED', r.processedAt = :processedAt
        WHERE r.refundId = :refundId
          AND r.status = 'PROCESSING'
          AND r.actorStaffId = :staffId
    """)
    int confirmCashPayout(@Param("refundId") Long refundId,
                          @Param("staffId") Long staffId,
                          @Param("processedAt") LocalDateTime processedAt);
}
