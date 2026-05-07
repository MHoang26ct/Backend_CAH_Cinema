package com.uit.backend_cinema.modules.booking.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateBookingResponseDTO {
    private Long bookingId;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private BigDecimal seatSubtotal;
    private BigDecimal foodSubtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
}
