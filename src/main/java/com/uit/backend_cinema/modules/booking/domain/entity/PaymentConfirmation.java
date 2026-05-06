package com.uit.backend_cinema.modules.booking.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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