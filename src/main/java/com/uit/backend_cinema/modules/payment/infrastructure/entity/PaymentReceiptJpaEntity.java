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

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "payment_receipts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payment_receipts_gateway_trans",
                        columnNames = {"gateway", "transaction_reference"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PaymentReceiptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_receipt_id")
    private Long paymentReceiptId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "payment_request_id")
    private Long paymentRequestId;

    @Column(name = "gateway", nullable = false, length = 30)
    private String gateway;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(name = "order_id", length = 200)
    private String orderId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentReceiptStatus status = PaymentReceiptStatus.RECORDED;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
