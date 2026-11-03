package com.uit.backend_cinema.modules.booking.api.dto;

import java.util.List;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.ticket.api.dto.TicketItemResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusResponseDTO {
    private Long bookingId;
    private BookingStatus status;
    private List<TicketItemResponseDTO> tickets;
}
