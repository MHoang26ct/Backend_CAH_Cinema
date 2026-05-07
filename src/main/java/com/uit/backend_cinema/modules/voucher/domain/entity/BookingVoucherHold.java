package com.uit.backend_cinema.modules.voucher.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

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
