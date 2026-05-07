package com.uit.backend_cinema.modules.outbox.domain.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingPaidPayload {
    private Long bookingId;
    private Long userId;
    private String paymentRef;
    private Long showtimeId;
}
