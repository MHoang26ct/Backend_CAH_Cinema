package com.uit.backend_cinema.modules.booking.api.dto;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmPaymentResponseDTO {
    private Long bookingId;
    private BookingStatus status;
    private String paymentRef;
    private String gateway;
    private String ticketStatus;
}
