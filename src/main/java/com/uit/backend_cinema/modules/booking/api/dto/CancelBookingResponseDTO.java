package com.uit.backend_cinema.modules.booking.api.dto;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponseDTO {
    private Long bookingId;
    private BookingStatus status;
    private LocalDateTime cancelledAt;
    private Long refundId;
    private BigDecimal refundAmount;
    private BigDecimal retainedAmount;
    private RefundStatus refundStatus;
    private BookingPaymentMethod paymentMethod;
    private String message;
}
