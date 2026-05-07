package com.uit.backend_cinema.modules.booking.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrePaymentBookingQuote {
    private Long bookingId;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private BigDecimal seatSubtotal;
    private BigDecimal foodSubtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
}
