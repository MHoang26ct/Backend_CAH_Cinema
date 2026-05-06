package com.uit.backend_cinema.modules.booking.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BookingVoucherHold {
    private Long bookingVoucherHoldId;
    private Long bookingId;
    private Long voucherId;
    private BigDecimal discountAmount;
    private BookingVoucherHoldStatus status;
    private LocalDateTime expiresAt;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}
