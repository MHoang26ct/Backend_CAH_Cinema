package com.uit.backend_cinema.modules.booking.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmation {
    private Long paymentConfirmationId;
    private String paymentRef;
    private Long bookingId;
    private PaymentConfirmationStatus status;
    private String gateway;
    private LocalDateTime createdAt;
}
