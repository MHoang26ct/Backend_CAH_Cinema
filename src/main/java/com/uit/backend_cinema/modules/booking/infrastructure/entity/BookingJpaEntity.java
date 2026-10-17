package com.uit.backend_cinema.modules.booking.infrastructure.entity;

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
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingSource;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.FoodFulfillmentStatus;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "late_discount_amount", nullable = false)
    private BigDecimal lateDiscountAmount = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "reconciliation_deadline")
    private LocalDateTime reconciliationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false, length = 20)
    private BookingSource bookingSource = BookingSource.ONLINE;

    @Column(name = "created_by_staff_id")
    private Long createdByStaffId;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "seat_subtotal_gross")
    private BigDecimal seatSubtotalGross = BigDecimal.ZERO;

    @Column(name = "food_subtotal_gross")
    private BigDecimal foodSubtotalGross = BigDecimal.ZERO;

    @Column(name = "rank_discount_amount")
    private BigDecimal rankDiscountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_discount_seat")
    private BigDecimal voucherDiscountSeat = BigDecimal.ZERO;

    @Column(name = "voucher_discount_food")
    private BigDecimal voucherDiscountFood = BigDecimal.ZERO;

    @Column(name = "net_seat_amount")
    private BigDecimal netSeatAmount = BigDecimal.ZERO;

    @Column(name = "net_food_amount")
    private BigDecimal netFoodAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_fulfillment_status", nullable = false, length = 20)
    private FoodFulfillmentStatus foodFulfillmentStatus = FoodFulfillmentStatus.NOT_APPLICABLE;

    @Column(name = "food_fulfilled_at")
    private LocalDateTime foodFulfilledAt;

    @Column(name = "food_fulfilled_by_staff_id")
    private Long foodFulfilledByStaffId;

    @Column(name = "cancellation_policy_snapshot", columnDefinition = "TEXT")
    private String cancellationPolicySnapshot;

    @Column(name = "is_voucher_released", nullable = false)
    private Boolean isVoucherReleased = false;

    @Column(name = "is_points_accumulated", nullable = false)
    private Boolean isPointsAccumulated = false;

    @Column(name = "is_points_reversed", nullable = false)
    private Boolean isPointsReversed = false;

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
