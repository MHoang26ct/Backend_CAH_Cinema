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
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private Boolean isDeleted;
}
