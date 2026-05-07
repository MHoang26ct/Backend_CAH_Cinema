package com.uit.backend_cinema.modules.booking.infrastructure.entity;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "payment_confirmations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payment_confirmations_payment_ref",
                        columnNames = "payment_ref"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PaymentConfirmationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_confirmation_id")
    private Long paymentConfirmationId;

    @Column(name = "payment_ref", nullable = false, length = 100)
    private String paymentRef;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentConfirmationStatus status;

    @Column(name = "gateway", nullable = false, length = 30)
    private String gateway;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
