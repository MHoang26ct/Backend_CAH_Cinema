package com.uit.backend_cinema.modules.booking.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Booking {
    private Long bookingId;
    private Long userId;
    private Long showtimeId;
    private Long voucherId;
    private BookingPaymentMethod paymentMethod;
    private BigDecimal discountAmount;
    private BigDecimal lateDiscountAmount;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime paymentDeadline;
    private LocalDateTime reconciliationDeadline;
    private BookingSource bookingSource = BookingSource.ONLINE;
    private Long createdByStaffId;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private BigDecimal seatSubtotalGross = BigDecimal.ZERO;
    private BigDecimal foodSubtotalGross = BigDecimal.ZERO;
    private BigDecimal rankDiscountAmount = BigDecimal.ZERO;
    private BigDecimal voucherDiscountSeat = BigDecimal.ZERO;
    private BigDecimal voucherDiscountFood = BigDecimal.ZERO;
    private BigDecimal netSeatAmount = BigDecimal.ZERO;
    private BigDecimal netFoodAmount = BigDecimal.ZERO;
    private FoodFulfillmentStatus foodFulfillmentStatus = FoodFulfillmentStatus.NOT_APPLICABLE;
    private LocalDateTime foodFulfilledAt;
    private Long foodFulfilledByStaffId;
    private String cancellationPolicySnapshot;
    private Boolean isVoucherReleased = false;
    private Boolean isPointsAccumulated = false;
    private Boolean isPointsReversed = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private Boolean isDeleted;
}
