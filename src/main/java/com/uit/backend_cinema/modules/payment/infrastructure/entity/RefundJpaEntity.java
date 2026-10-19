package com.uit.backend_cinema.modules.payment.infrastructure.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_refunds_receipt_cause",
                        columnNames = {"payment_receipt_id", "cause"}
                ),
                @UniqueConstraint(
                        name = "uq_refunds_request_ref",
                        columnNames = "request_reference"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class RefundJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "payment_receipt_id", nullable = false)
    private Long paymentReceiptId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cause", nullable = false, length = 50)
    private RefundCause cause;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "policy_snapshot", columnDefinition = "TEXT")
    private String policySnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "request_reference", nullable = false, length = 100)
    private String requestReference;

    @Column(name = "actor_staff_id")
    private Long actorStaffId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "gateway_response_code", length = 50)
    private String gatewayResponseCode;

    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;

    @Column(name = "gateway_response_message", columnDefinition = "TEXT")
    private String gatewayResponseMessage;

    @Column(name = "membership_adjusted_at")
    private LocalDateTime membershipAdjustedAt;

    @Column(name = "membership_adjustment_reversed_at")
    private LocalDateTime membershipAdjustmentReversedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
