package com.uit.backend_cinema.modules.outbox.domain.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendTicketEmailPayload {
    private Long bookingId;
    private Long userId;
}
