package com.uit.backend_cinema.modules.booking.infrastructure.entity;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BookingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private BookingPaymentMethod paymentMethod;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
