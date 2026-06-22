package com.uit.backend_cinema.modules.booking.api.dto;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingStatusResponseDTO {
    private Long bookingId;
    private BookingStatus status;
}
