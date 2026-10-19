package com.uit.backend_cinema.modules.ticket.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.uit.backend_cinema.modules.booking.domain.entity.FoodFulfillmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponseDTO {
    private Long ticketId;
    private Long bookingId;
    private String movieTitle;
    private String cinemaName;
    private String roomName;
    private LocalDateTime showtimeStart;
    private String seatName;
    private FoodFulfillmentStatus foodFulfillmentStatus;
    private Boolean foodDeliveredNow;
    private List<CheckInFoodItemDTO> foodItems;
}
