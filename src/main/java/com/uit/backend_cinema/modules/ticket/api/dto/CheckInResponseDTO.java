package com.uit.backend_cinema.modules.ticket.api.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckInResponseDTO {
    private Long ticketId;
    private Long bookingId;
    private String movieTitle;
    private String cinemaName;
    private String roomName;
    private LocalDateTime showtimeStart;
    private String seatName;
}
